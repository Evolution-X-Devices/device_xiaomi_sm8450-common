/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * libSNPE.so is built with NDK r19c: it links the libgcc unwinder
 * statically (its _Unwind_Resume is local) and expects the C++ ABI of
 * the libc++_shared.so it ships with, which uses the same libgcc
 * unwinder internally. Its C++ ABI imports are resolved in the global
 * scope first, though, so they bind to the platform /vendor/lib64/libc++.so:
 * exceptions are raised by the LLVM unwinder of bionic, then resumed
 * by the libgcc copy of libSNPE, which calls the platform personality
 * with a libgcc context. On Android 17 this jumps to the stack.
 *
 * The extract-files fixup renames these imports to snpe_cxxabi_<name>.
 * The trampolines below forward them to libc++_shared.so, so libSNPE
 * throws, catches and resumes with a single unwinder.
 */

#define LOG_TAG "snpe_cxxabi"

#include <dlfcn.h>
#include <log/log.h>
#include <stddef.h>

typedef void* (*generic_fn)(void*, void*, void*, void*, void*, void*, void*, void*);

#define SNPE_CXXABI_SYMBOLS(X)                  \
    X(__gxx_personality_v0)                     \
    X(__cxa_allocate_exception)                 \
    X(__cxa_free_exception)                     \
    X(__cxa_throw)                              \
    X(__cxa_rethrow)                            \
    X(__cxa_begin_catch)                        \
    X(__cxa_end_catch)                          \
    X(__cxa_bad_cast)                           \
    X(_ZSt17current_exceptionv)                 \
    X(_ZSt17rethrow_exceptionSt13exception_ptr) \
    X(_ZSt18uncaught_exceptionv)                \
    X(_ZNSt13exception_ptrC1ERKS_)              \
    X(_ZNSt13exception_ptrD1Ev)                 \
    X(_ZNSt13exception_ptraSERKS_)              \
    X(_Znwm)                                    \
    X(_Znam)

#define DECLARE_POINTER(name) static generic_fn real_##name;
SNPE_CXXABI_SYMBOLS(DECLARE_POINTER)

static void snpe_cxxabi_resolve(void) {
    void* handle = dlopen("libc++_shared.so", RTLD_NOW | RTLD_NOLOAD);
    if (handle == NULL) {
        handle = dlopen("libc++_shared.so", RTLD_NOW);
    }
    if (handle == NULL) {
        ALOGE("failed to open libc++_shared.so: %s", dlerror());
    }

#define RESOLVE(name)                                                    \
    if (handle != NULL) real_##name = (generic_fn)dlsym(handle, #name); \
    if (real_##name == NULL) {                                           \
        ALOGE("%s not found in libc++_shared.so, using the global one", \
              #name);                                                    \
        real_##name = (generic_fn)dlsym(RTLD_DEFAULT, #name);            \
    }
    SNPE_CXXABI_SYMBOLS(RESOLVE)
#undef RESOLVE
}

__attribute__((constructor)) static void snpe_cxxabi_init(void) {
    snpe_cxxabi_resolve();
}

#define DEFINE_TRAMPOLINE(name)                                                     \
    __attribute__((visibility("default"))) void* snpe_cxxabi_##name(                \
            void* a0, void* a1, void* a2, void* a3, void* a4, void* a5, void* a6,   \
            void* a7) {                                                             \
        if (real_##name == NULL) snpe_cxxabi_resolve();                             \
        __attribute__((musttail)) return real_##name(a0, a1, a2, a3, a4, a5, a6, a7); \
    }
SNPE_CXXABI_SYMBOLS(DEFINE_TRAMPOLINE)
