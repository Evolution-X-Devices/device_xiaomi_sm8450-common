/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.saturation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.theme.SettingsSpace
import org.lineageos.settings.R
import org.lineageos.settings.saturation.SaturationTileService
import org.lineageos.settings.saturation.SaturationUtils
import org.lineageos.settings.ui.ExpressiveFooter
import org.lineageos.settings.ui.ExpressiveGroup
import org.lineageos.settings.ui.ExpressiveIcon
import org.lineageos.settings.ui.ExpressiveRow
import org.lineageos.settings.ui.ExpressiveScaffold
import org.lineageos.settings.ui.ExpressiveScreen
import org.lineageos.settings.ui.ExpressiveSliderRow
import org.lineageos.settings.utils.TileUtils

private const val DEFAULT_SATURATION = 100

private val previews = intArrayOf(
    R.drawable.image_preview1,
    R.drawable.image_preview2,
    R.drawable.image_preview3,
)

@Composable
fun SaturationScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    var value by remember { mutableIntStateOf(SaturationUtils.get(context)) }

    fun commit(newValue: Int) {
        SaturationUtils.apply(newValue)
        SaturationUtils.save(context, newValue)
    }

    // Re-apply what is saved: the color mode changing in display settings resets it.
    LaunchedEffect(Unit) { SaturationUtils.apply(value) }

    ExpressiveScaffold(
        title = stringResource(R.string.saturation_title),
        onBack = onBackPressed,
    ) { padding ->
        ExpressiveScreen(modifier = Modifier.padding(padding)) {
            SaturationPreview()
            ExpressiveGroup(title = "") {
                item {
                    ExpressiveSliderRow(
                        title = stringResource(R.string.saturation_title),
                        value = value,
                        valueRange = 0..200,
                        valueLabel = { "$it%" },
                        onValueChange = {
                            value = it
                            SaturationUtils.apply(it)
                        },
                        onValueChangeFinished = { SaturationUtils.save(context, it) },
                    )
                }
                item {
                    ExpressiveRow(
                        title = stringResource(R.string.saturation_reset),
                        summary = "$DEFAULT_SATURATION%",
                        enabled = value != DEFAULT_SATURATION,
                        icon = Icons.Rounded.Refresh,
                        onClick = {
                            value = DEFAULT_SATURATION
                            commit(DEFAULT_SATURATION)
                        },
                    )
                }
                item {
                    ExpressiveRow(
                        title = stringResource(R.string.tile_add),
                        leading = { ExpressiveIcon(R.drawable.ic_saturation_tile) },
                        onClick = {
                            TileUtils.requestAddTileService(
                                context,
                                SaturationTileService::class.java,
                                R.string.saturation_title,
                                R.drawable.ic_saturation_tile,
                            )
                        },
                    )
                }
            }
            ExpressiveFooter(stringResource(R.string.saturation_footer_summary))
        }
    }
}

/** Swipeable sample photos with page dots, in the container shape of the other cards. */
@Composable
private fun SaturationPreview() {
    val pagerState = rememberPagerState { previews.size }
    Box(
        modifier = Modifier
            .padding(horizontal = SettingsSpace.small1, vertical = SettingsSpace.extraSmall4)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            Image(
                painter = painterResource(previews[page]),
                contentDescription = stringResource(R.string.image_preview_content_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = SettingsSpace.extraSmall6),
            horizontalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall3),
        ) {
            repeat(previews.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceBright,
                        ),
                )
            }
        }
    }
}
