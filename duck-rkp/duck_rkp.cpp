/* SPDX-License-Identifier: MIT
 * Native CSR framing and KeymasterUtils ABI adapted from eltavine/Duck-ToolBox
 * 8d6a2470b1033c35946b72bb1f9bc56d8947dd63, including aviraxp's native V1 contribution.
 * Copyright (c) 2026 Eltavine (尉迟栩澜)
 */
#include <arpa/inet.h>
#include <aidl/vendor/evo/duckrkp/BnDuckRkp.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <android/binder_ibinder.h>
#include <spawn.h>
#include <dlfcn.h>
#include <signal.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <unistd.h>

#include <array>
#include <chrono>
#include <climits>
#include <cstring>
#include <set>
#include <thread>
#include <vector>

#include <cppbor.h>
#include <cppbor_parse.h>

extern char** environ;

namespace {
constexpr uint32_t kMagic = 0x44524b50;
constexpr size_t kBufferSize = 0xa000;
constexpr size_t kMaxRequest = 0x9000;
constexpr const char* kLibrary = "/vendor/lib64/libkeymasterdeviceutils.so";

enum Status : uint32_t { OK, INVALID_REQUEST, UNAVAILABLE, TRANSPORT, REJECTED, TIMEOUT };

bool transfer(int fd, void* data, size_t size, bool writing) {
    auto* bytes = static_cast<uint8_t*>(data);
    while (size) {
        ssize_t count = writing ? send(fd, bytes, size, MSG_NOSIGNAL) : recv(fd, bytes, size, 0);
        if (count < 0 && errno == EINTR) continue;
        if (count <= 0) return false;
        bytes += count;
        size -= count;
    }
    return true;
}

void reply(int fd, Status status, int32_t detail, const std::vector<uint8_t>& payload = {}) {
    std::array<uint32_t, 3> header{htonl(status), htonl(static_cast<uint32_t>(detail)),
                                 htonl(payload.size())};
    if (transfer(fd, header.data(), sizeof(header), true) && !payload.empty()) {
        transfer(fd, const_cast<uint8_t*>(payload.data()), payload.size(), true);
    }
}

std::unique_ptr<cppbor::Item> decode(const std::vector<uint8_t>& bytes) {
    if (bytes.empty() || bytes.size() > kMaxRequest) return nullptr;
    auto [item, end, error] = cppbor::parse(bytes);
    if (!item || end != bytes.data() + bytes.size()) return nullptr;
    return std::move(item);
}

bool byteField(const cppbor::Map& map, int key, size_t minimum, size_t maximum) {
    const auto& field = map.get(key);
    return field && field->asBstr() && field->asBstr()->value().size() >= minimum &&
           field->asBstr()->value().size() <= maximum;
}

bool integerField(const cppbor::Map& map, int key, int64_t expected) {
    const auto& field = map.get(key);
    return field && field->asInt() && field->asInt()->value() == expected;
}

bool validRequest(uint32_t backend, const std::vector<uint8_t>& payload) {
    auto item = decode(payload);
    if (!item || !item->asMap()) return false;
    const auto& map = *item->asMap();
    std::set<int64_t> labels;
    for (const auto& [key, value] : map) {
        if (!key->asInt() || !labels.insert(key->asInt()->value()).second) return false;
    }
    if (!byteField(map, 4, 1, 64)) return false;
    if (backend == 2) {
        if (labels != std::set<int64_t>{4, 57} || !byteField(map, 57, 1, 256)) return false;
        auto keys = decode(map.get(57)->asBstr()->value());
        if (!keys || !keys->asArray() || keys->asArray()->size() != 1) return false;
        const auto* key = (*keys->asArray())[0]->asMap();
        return key && key->size() == 5 && integerField(*key, 1, 2) &&
               integerField(*key, 3, -7) && integerField(*key, -1, 1) &&
               byteField(*key, -2, 32, 32) && byteField(*key, -3, 32, 32);
    }
    if (backend != 1 || labels != std::set<int64_t>{4, 55, 56, 57, 58} ||
        !integerField(map, 55, 0) || !byteField(map, 56, 32, 32) ||
        !byteField(map, 57, 32, 32) || !byteField(map, 58, 1, 8192)) return false;
    auto chain = decode(map.get(58)->asBstr()->value());
    if (!chain || !chain->asArray() || chain->asArray()->size() < 1 ||
        chain->asArray()->size() > 8) return false;
    for (const auto& cert : *chain->asArray()) {
        const auto* fields = cert->asArray();
        if (!fields || fields->size() != 4 || !(*fields)[0]->asBstr() ||
            !(*fields)[1]->asMap() || !(*fields)[2]->asBstr() ||
            !(*fields)[3]->asBstr()) return false;
    }
    return true;
}

std::vector<uint8_t> encodeVendorRequest(uint32_t backend, const std::vector<uint8_t>& payload) {
    auto body = payload;
    if (backend == 1) {
        auto item = decode(payload);
        const auto& map = *item->asMap();
        // The QTI V1 decoder consumes fields sequentially, even though this is a CBOR map.
        body = cppbor::Map().add(55, map.get(55)->clone()).add(4, map.get(4)->clone())
                .add(56, map.get(56)->clone()).add(57, map.get(57)->clone())
                .add(58, map.get(58)->clone()).encode();
    }
    std::vector<uint8_t> request(4 + body.size());
    uint32_t command = htole32(backend == 2 ? 0x3122 : 0x311e);
    memcpy(request.data(), &command, 4);
    memcpy(request.data() + 4, body.data(), body.size());
    return request;
}

void vendorRequest(int fd, uint32_t backend, const std::vector<uint8_t>& payload) {
    void* library = dlopen(kLibrary, RTLD_NOW | RTLD_LOCAL);
    if (!library) return reply(fd, UNAVAILABLE, 0);
    using Constructor = void (*)(void*, int);
    using Destructor = void (*)(void*);
    using Send = int (*)(void*, void*, uint32_t, void*, uint32_t);
    auto construct = reinterpret_cast<Constructor>(dlsym(library,
            "_ZN14keymasterutils14KeymasterUtilsC1E26keymaster_security_level_t"));
    auto destroy = reinterpret_cast<Destructor>(dlsym(library,
            "_ZN14keymasterutils14KeymasterUtilsD1Ev"));
    auto sendCommand = reinterpret_cast<Send>(dlsym(library,
            "_ZN14keymasterutils14KeymasterUtils8send_cmdEPvjS1_j"));
    if (!construct || !destroy || !sendCommand) return reply(fd, UNAVAILABLE, 1);
    // Observed vendor ABI: 0x40-byte object, security level 1 (TEE).
    alignas(16) std::array<uint8_t, 0x40> object{};
    construct(object.data(), 1);
    auto request = encodeVendorRequest(backend, payload);
    std::vector<uint8_t> response(kBufferSize);
    int32_t sentinel = INT32_MIN;
    memcpy(response.data(), &sentinel, 4);
    int status = sendCommand(object.data(), request.data(), request.size(),
                             response.data(), response.size());
    destroy(object.data());
    dlclose(library);
    if (status != 0) return reply(fd, TRANSPORT, status);
    int32_t result;
    uint32_t size;
    memcpy(&result, response.data(), 4);
    memcpy(&size, response.data() + 4, 4);
    result = static_cast<int32_t>(le32toh(static_cast<uint32_t>(result)));
    size = le32toh(size);
    if (result != 0) return reply(fd, REJECTED, result);
    if (size == 0 || size > kBufferSize - 8) return reply(fd, TRANSPORT, 0);
    reply(fd, OK, 0, {response.begin() + 8, response.begin() + 8 + size});
}

void serve(int fd) {
    alarm(30);
    timeval timeout{5, 0};
    setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));
    std::array<uint32_t, 3> header;
    if (!transfer(fd, header.data(), sizeof(header), false)) return;
    uint32_t backend = ntohl(header[1]), size = ntohl(header[2]);
    if (ntohl(header[0]) != kMagic || size == 0 || size > kMaxRequest - 4) {
        return reply(fd, INVALID_REQUEST, 0);
    }
    std::vector<uint8_t> payload(size);
    if (!transfer(fd, payload.data(), size, false) || !validRequest(backend, payload)) {
        return reply(fd, INVALID_REQUEST, 0);
    }
    vendorRequest(fd, backend, payload);
}

int selfTest() {
    auto publicKey = cppbor::Map().add(1, 2).add(3, -7).add(-1, 1)
            .add(-2, std::vector<uint8_t>(32, 1)).add(-3, std::vector<uint8_t>(32, 2));
    auto keys = cppbor::Array().add(std::move(publicKey)).encode();
    auto valid = cppbor::Map().add(4, std::vector<uint8_t>{1}).add(57, keys).encode();
    if (!validRequest(2, valid) || validRequest(0x3122, valid) || validRequest(1, valid)) return 1;
    valid.push_back(0);
    if (validRequest(2, valid) || validRequest(2, {}) ||
        validRequest(2, std::vector<uint8_t>(kMaxRequest + 1))) return 2;
    auto empty = cppbor::Map().add(4, std::vector<uint8_t>{}).add(57, keys).encode();
    if (validRequest(2, empty)) return 3;
    auto chain = cppbor::Array().add(cppbor::Array().add(std::vector<uint8_t>{0xa0})
            .add(cppbor::Map()).add(std::vector<uint8_t>{0xa0})
            .add(std::vector<uint8_t>(64))).encode();
    auto legacy = [&](int mode, size_t macSize, const std::vector<uint8_t>& eek) {
        return cppbor::Map().add(4, std::vector<uint8_t>{1}).add(55, mode)
                .add(56, std::vector<uint8_t>(macSize, 0x56)).add(57, std::vector<uint8_t>(32, 0x57))
                .add(58, eek).encode();
    };
    if (!validRequest(1, legacy(0, 32, chain)) || validRequest(2, legacy(0, 32, chain)) ||
        validRequest(0x220a, legacy(0, 32, chain))) return 4;
    if (validRequest(1, legacy(1, 32, chain)) || validRequest(1, legacy(0, 31, chain)) ||
        validRequest(1, legacy(0, 32, cppbor::Array().encode()))) return 5;
    auto legacyPayload = legacy(0, 32, chain);
    auto request = encodeVendorRequest(1, legacyPayload);
    const uint8_t prefix[] = {0x1e, 0x31, 0x00, 0x00, 0xa5, 0x18, 0x37, 0x00, 0x04};
    if (request.size() < sizeof(prefix) || memcmp(request.data(), prefix, sizeof(prefix))) return 6;
    std::vector<uint8_t> body(request.begin() + 4, request.end());
    if (!validRequest(1, body)) return 7;
    auto ordered = decode(body);
    auto original = decode(legacyPayload);
    const std::array<int64_t, 5> order{55, 4, 56, 57, 58};
    size_t index = 0;
    for (const auto& [key, value] : *ordered->asMap()) {
        if (key->asInt()->value() != order[index++] ||
            value->encode() != original->asMap()->get(key->asInt()->value())->encode()) return 8;
    }
    auto v2Payload = cppbor::Map().add(4, std::vector<uint8_t>{1}).add(57, keys).encode();
    std::vector<uint8_t> expectedV2{0x22, 0x31, 0x00, 0x00};
    expectedV2.insert(expectedV2.end(), v2Payload.begin(), v2Payload.end());
    if (encodeVendorRequest(2, v2Payload) != expectedV2) return 9;
    return 0;
}
std::vector<uint8_t> errorReply(Status status, int32_t detail = 0) {
    std::array<uint32_t, 3> header{htonl(status), htonl(static_cast<uint32_t>(detail)), 0};
    auto* data = reinterpret_cast<const uint8_t*>(header.data());
    return {data, data + sizeof(header)};
}

std::vector<uint8_t> runWorker(uint32_t backend, const std::vector<uint8_t>& payload) {
    if (!validRequest(backend, payload)) return errorReply(INVALID_REQUEST);
    int sockets[2];
    if (socketpair(AF_UNIX, SOCK_STREAM | SOCK_CLOEXEC, 0, sockets) != 0) {
        return errorReply(TRANSPORT, errno);
    }
    posix_spawn_file_actions_t actions;
    posix_spawn_file_actions_init(&actions);
    posix_spawn_file_actions_adddup2(&actions, sockets[1], STDIN_FILENO);
    if (sockets[0] != STDIN_FILENO) posix_spawn_file_actions_addclose(&actions, sockets[0]);
    if (sockets[1] != STDIN_FILENO) posix_spawn_file_actions_addclose(&actions, sockets[1]);
    char executable[] = "/vendor/bin/duck_rkp";
    char argument[] = "--worker";
    char* argv[] = {executable, argument, nullptr};
    pid_t worker;
    int result = posix_spawn(&worker, executable, &actions, nullptr, argv, environ);
    posix_spawn_file_actions_destroy(&actions);
    close(sockets[1]);
    if (result != 0) {
        close(sockets[0]);
        return errorReply(TRANSPORT, result);
    }
    timeval timeout{35, 0};
    setsockopt(sockets[0], SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    setsockopt(sockets[0], SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));
    std::array<uint32_t, 3> header{htonl(kMagic), htonl(backend), htonl(payload.size())};
    bool complete = transfer(sockets[0], header.data(), sizeof(header), true)
            && transfer(sockets[0], const_cast<uint8_t*>(payload.data()), payload.size(), true)
            && transfer(sockets[0], header.data(), sizeof(header), false);
    size_t size = complete ? ntohl(header[2]) : 0;
    std::vector<uint8_t> response;
    if (complete && size <= kBufferSize - 8) {
        const auto* data = reinterpret_cast<const uint8_t*>(header.data());
        response.assign(data, data + sizeof(header));
        response.resize(sizeof(header) + size);
        complete = transfer(sockets[0], response.data() + sizeof(header), size, false);
    } else {
        complete = false;
    }
    close(sockets[0]);
    if (!complete) kill(worker, SIGKILL);
    int status;
    while (waitpid(worker, &status, 0) < 0) {
        if (errno != EINTR) return errorReply(TRANSPORT, errno);
    }
    if (WIFSIGNALED(status)) {
        return errorReply(WTERMSIG(status) == SIGALRM ? TIMEOUT : TRANSPORT, WTERMSIG(status));
    }
    if (!complete || !WIFEXITED(status) || WEXITSTATUS(status) != 0) {
        return errorReply(TRANSPORT);
    }
    return response;
}

class DuckRkp : public aidl::vendor::evo::duckrkp::BnDuckRkp {
    ndk::ScopedAStatus request(int32_t backend, const std::vector<uint8_t>& payload,
                              std::vector<uint8_t>* output) override {
        if (AIBinder_getCallingUid() != 1000) {
            return ndk::ScopedAStatus::fromExceptionCode(EX_SECURITY);
        }
        *output = runWorker(backend, payload);
        return ndk::ScopedAStatus::ok();
    }
};
}  // namespace

int main(int argc, char** argv) {
    rlimit core{0, 0};
    setrlimit(RLIMIT_CORE, &core);
    prctl(PR_SET_DUMPABLE, 0);
    if (argc == 2 && strcmp(argv[1], "--self-test") == 0) return selfTest();
    if (argc == 2 && strcmp(argv[1], "--worker") == 0) {
        prctl(PR_SET_PDEATHSIG, SIGKILL);
        if (getppid() == 1) return 1;
        serve(STDIN_FILENO);
        return 0;
    }
    if (argc != 1) return 1;
    ABinderProcess_setThreadPoolMaxThreadCount(0);
    auto service = ndk::SharedRefBase::make<DuckRkp>();
    std::string name = std::string(DuckRkp::descriptor) + "/default";
    if (AServiceManager_addService(service->asBinder().get(), name.c_str()) != STATUS_OK) return 1;
    ABinderProcess_joinThreadPool();
    return 1;
}
