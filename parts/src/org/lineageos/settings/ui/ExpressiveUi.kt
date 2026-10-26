/*
 * SPDX-FileCopyrightText: Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.lineageos.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.annotation.DrawableRes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsRadius
import com.android.settingslib.spa.framework.theme.SettingsShape
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.framework.theme.isSpaExpressiveEnabled
import com.android.settingslib.spa.framework.theme.surfaceTone
import com.android.settingslib.spa.widget.dialog.getDialogWidth
import com.android.settingslib.spa.widget.ui.CategoryTitle
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Shared Material 3 Expressive building blocks for the Parts screens.
//
// Trimmed from Evolver's ExpressiveUi.kt (packages_apps_Evolver, 60a30f0) and
// kept close to it so the two stay uniform: SettingsLib SPA tokens, 16dp
// screen inset, 20dp grouped-card corners with 2dp gaps between rows,
// surfaceBright row containers and full-pill buttons.
//
// Left out on purpose: scrollbar, focus rings and the font-aware typography
// (Settings-only helpers), and the pickers, text fields and app lists.
// ---------------------------------------------------------------------------

/** Semantic colour roles for [ExpressiveStatusChip]. Mapped onto theme roles, never hard-coded. */
enum class ExpressiveTone { Positive, Warning, Negative, Neutral }

/** Scrollable page body shared by the Expressive screens. */
@Composable
fun ExpressiveScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = SettingsSpace.extraSmall4, bottom = SettingsSpace.medium1),
        content = content,
    )
}

/** Horizontal screen inset for items that live outside a [ExpressiveGroup]. */
fun Modifier.expressiveInset(): Modifier = padding(horizontal = SettingsSpace.small1)

/** Top app bar with a back arrow over the page [content], on the theme background. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        content = content,
    )
}

/** Explanatory text below the last group of a screen. */
@Composable
fun ExpressiveFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsSpace.small4,
                vertical = SettingsSpace.small1,
            ),
    )
}

/** Row icon from a drawable resource, tinted and sized like the XML preference icons. */
@Composable
fun ExpressiveIcon(@DrawableRes id: Int) {
    Icon(
        painter = painterResource(id),
        contentDescription = null,
        modifier = Modifier.size(SettingsDimension.itemIconSize),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Theme for every Expressive screen: SettingsTheme (SPA colours, typography, shapes) re-themed with
 * [MaterialExpressiveTheme] and the expressive [MotionScheme]. The colour scheme, shapes and
 * typography are passed explicitly: outside a nested expressive theme, [MaterialExpressiveTheme]
 * would otherwise fall back to its own defaults.
 */
@Composable
fun ExpressiveTheme(content: @Composable () -> Unit) {
    SettingsTheme {
        MaterialExpressiveTheme(
            colorScheme = MaterialTheme.colorScheme,
            shapes = MaterialTheme.shapes,
            typography = MaterialTheme.typography,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

/**
 * Pill buttons with an explicit pressed shape. [ButtonDefaults.shapes] resolves its shapes from
 * the ambient theme at the time the button recomposes, so pin both shapes: a pill at rest and a
 * rounded rectangle while pressed.
 */
@Composable
fun expressiveButtonShapes(): ButtonShapes = ButtonDefaults.shapes(
    shape = SettingsShape.CornerFull,
    pressedShape = RoundedCornerShape(SettingsRadius.large2),
)

/** Shapes for the row currently being drawn by a [ExpressiveGroup]; null outside of a group. */
private val LocalExpressiveRowShapes = compositionLocalOf<ListItemShapes?> { null }

/** Segmented shapes of the enclosing group row, or a standalone (all outer corners) row. */
@Composable
private fun currentExpressiveRowShapes(): ListItemShapes =
    LocalExpressiveRowShapes.current ?: expressiveRowShapes(index = 0, count = 1)

/**
 * Corner roles shared by every Expressive container, taken from SettingsLib's expressive
 * preference backgrounds: 20dp outer corners, 4dp inner corners. Buttons and chips stay fully
 * rounded; containers never do.
 */
object ExpressiveCorners {
    private val outer = SettingsRadius.large2
    private val inner = SettingsRadius.extraSmall2

    /** Standalone card or lone row: all four corners are outer corners. */
    val single = RoundedCornerShape(outer)
    val first = RoundedCornerShape(
        topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner,
    )
    val middle = RoundedCornerShape(inner)
    val last = RoundedCornerShape(
        topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer,
    )
}

/** 16dp at the sides and above and below the text frame, inside a 72dp minimum height. */
internal val ExpressiveRowPadding = PaddingValues(
    horizontal = SettingsSpace.small1,
    vertical = SettingsSpace.small1,
)

/**
 * Shapes for row [index] of [count], with SettingsLib's 20dp / 4dp corners. A pressed row morphs
 * to the standalone shape, as in stock Material 3 Expressive.
 */
@Composable
fun expressiveRowShapes(index: Int, count: Int): ListItemShapes {
    val resting = when {
        count <= 1 -> ExpressiveCorners.single
        index == 0 -> ExpressiveCorners.first
        index == count - 1 -> ExpressiveCorners.last
        else -> ExpressiveCorners.middle
    }
    return ListItemDefaults.shapes(
        shape = resting,
        selectedShape = resting,
        pressedShape = ExpressiveCorners.single,
        focusedShape = resting,
        hoveredShape = resting,
    )
}

/**
 * Gives the rows inside [content] the segment shapes of row [index] of [count]. [ExpressiveGroup]
 * does this itself; use it directly for rows laid out by a lazy list.
 */
@Composable
fun ExpressiveSegment(index: Int, count: Int, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalExpressiveRowShapes provides expressiveRowShapes(index = index, count = count),
    ) { content() }
}

/** Builder passed to [ExpressiveGroup]; every [item] becomes one segment of the group. */
class ExpressiveGroupScope internal constructor() {
    internal val items = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        items += content
    }
}

/**
 * A titled group of rows: [CategoryTitle] above a column of segmented rows (large outer corners,
 * small inner corners, 2dp gaps). Each row is declared with [ExpressiveGroupScope.item] so the
 * group knows its index and count and can hand every row the right shapes.
 */
@Composable
fun ExpressiveGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: ExpressiveGroupScope.() -> Unit,
) {
    val items = ExpressiveGroupScope().apply(content).items
    Column(
        modifier = modifier.padding(
            horizontal = SettingsSpace.small1,
            vertical = SettingsSpace.extraSmall4,
        ),
    ) {
        if (title.isNotEmpty()) CategoryTitle(title)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall1),
        ) {
            items.forEachIndexed { index, rowContent ->
                ExpressiveSegment(index = index, count = items.size, content = rowContent)
            }
        }
    }
}

/**
 * Shared list item: a [SegmentedListItem] (pressed-shape morph, ripple, disabled colours) when
 * [onClick] is set, or a static surface with the same shape and layout when it is not.
 */
@Composable
private fun ExpressiveListItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    overline: (@Composable () -> Unit)? = null,
    supporting: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    headline: @Composable () -> Unit,
) {
    val shapes = currentExpressiveRowShapes()
    val container = MaterialTheme.colorScheme.surfaceBright
    val trailingFrame: (@Composable () -> Unit)? = trailing?.let { t ->
        { ExpressiveTrailingFrame(t) }
    }
    if (onClick != null) {
        SegmentedListItem(
            onClick = onClick,
            shapes = shapes,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = SettingsDimension.preferenceMinHeight),
            enabled = enabled,
            colors = ListItemDefaults.colors(containerColor = container),
            leadingContent = leading?.let { l -> { l() } },
            trailingContent = trailingFrame,
            overlineContent = overline,
            supportingContent = supporting,
            // The stock default top-aligns trailing content on tall rows; the XML rows never do.
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = ExpressiveRowPadding,
            content = headline,
        )
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { if (!enabled) disabled() }
                .alpha(if (enabled) 1f else 0.38f),
            shape = shapes.shape,
            color = container,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = SettingsDimension.preferenceMinHeight)
                    .padding(ExpressiveRowPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(SettingsDimension.itemPaddingStart))
                }
                Column(modifier = Modifier.weight(1f)) {
                    overline?.invoke()
                    headline()
                    if (supporting != null) {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) { supporting() }
                        }
                    }
                }
                if (trailingFrame != null) {
                    Spacer(Modifier.width(SettingsSpace.small1))
                    trailingFrame()
                }
            }
        }
    }
}

/**
 * Trailing slot with the footprint of SettingsLib's preference widget frame: at least 48dp wide,
 * content centred, so chevrons and chips share a centre line with the switches around them.
 */
@Composable
private fun ExpressiveTrailingFrame(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .trailingWidthCap()
            .widthIn(min = 48.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Caps trailing content at half of the width it is offered. Rows measure their trailing content
 * first and hand the title what is left, so an uncapped chip squeezes the title.
 */
internal fun Modifier.trailingWidthCap(): Modifier = layout { measurable, constraints ->
    val capped = if (constraints.hasBoundedWidth) {
        constraints.copy(maxWidth = maxOf(constraints.minWidth, constraints.maxWidth / 2))
    } else {
        constraints
    }
    val placeable = measurable.measure(capped)
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/**
 * Base row: surfaceBright segment, optional leading icon, title + summary and an optional
 * trailing slot. Disabled rows are dimmed and not clickable.
 */
@Composable
fun ExpressiveRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    ExpressiveListItem(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        leading = leading ?: icon?.let { vector ->
            {
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    modifier = Modifier.size(SettingsDimension.itemIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        supporting = summary?.takeIf { it.isNotEmpty() }?.let { text ->
            { Text(text = text) }
        },
        trailing = trailing,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}

/** Navigation row: [ExpressiveRow] with a trailing chevron, or a [chip] when one is supplied. */
@Composable
fun ExpressiveNavRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    chip: String? = null,
    chipTone: ExpressiveTone = ExpressiveTone.Neutral,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    ExpressiveRow(
        title = title,
        modifier = modifier,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        trailing = {
            if (chip != null) {
                ExpressiveStatusChip(chip, chipTone)
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/** Small pill used for status values ("On", "Auto", "Excluded"). */
@Composable
fun ExpressiveStatusChip(
    text: String,
    tone: ExpressiveTone = ExpressiveTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        ExpressiveTone.Positive -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        ExpressiveTone.Warning -> scheme.secondaryContainer to scheme.onSecondaryContainer
        ExpressiveTone.Negative -> scheme.errorContainer to scheme.onErrorContainer
        ExpressiveTone.Neutral -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = SettingsShape.CornerFull,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                horizontal = SettingsSpace.extraSmall5,
                vertical = SettingsSpace.extraSmall2,
            ),
        )
    }
}

/**
 * Shared Switch with a Check/Close thumb icon, matching SettingsLib's SettingsSwitch (internal to
 * SPA, so not linkable).
 */
@Composable
fun CheckCloseSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = if (isSpaExpressiveEnabled) {
            {
                Icon(
                    imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

/**
 * Switch row: a regular [ExpressiveRow] (a surfaceBright segment of the enclosing
 * [ExpressiveGroup]) with the shared check/close switch as the trailing slot.
 */
@Composable
fun ExpressiveSwitchRow(
    titleText: String,
    summaryText: String?,
    isChecked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
) {
    ExpressiveRow(
        title = titleText,
        modifier = modifier,
        summary = summaryText,
        icon = icon,
        leading = leading,
        enabled = isEnabled,
        onClick = { onChange(!isChecked) },
        trailing = {
            CheckCloseSwitch(
                checked = isChecked,
                onCheckedChange = onChange,
                enabled = isEnabled,
            )
        },
    )
}

/**
 * Slider row: title with the current value on the right and a [Slider] below, in the same
 * surfaceBright segment as every other row. [onValueChangeFinished] receives the final value.
 */
@Composable
fun ExpressiveSliderRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: IntRange = 0..100,
    valueLabel: (Int) -> String = { it.toString() },
    enabled: Boolean = true,
    onValueChangeFinished: ((Int) -> Unit)? = null,
) {
    var position by remember { mutableFloatStateOf(value.toFloat()) }
    // Follow external changes (reset) without fighting an active drag, which reports whole values.
    LaunchedEffect(value) { if (value != position.roundToInt()) position = value.toFloat() }
    val shapes = currentExpressiveRowShapes()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f),
        shape = shapes.shape,
        color = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(ExpressiveRowPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = valueLabel(position.roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = position,
                onValueChange = {
                    position = it
                    onValueChange(it.roundToInt())
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                onValueChangeFinished = { onValueChangeFinished?.invoke(position.roundToInt()) },
                colors = SliderDefaults.colors(
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceTone,
                ),
            )
        }
    }
}

/**
 * Expressive text button: [TextButton] with the pressed-shape morph of [ButtonDefaults.shapes].
 * Used for every dialog action on the Expressive screens.
 */
@Composable
fun ExpressiveTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        shapes = expressiveButtonShapes(),
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content,
    )
}

/**
 * Shared dialog for every Expressive screen: SPA dialog width, extra-large corners and a
 * surfaceContainerHigh container so rows stay visible inside it. Keep
 * `usePlatformDefaultWidth = false` so the SPA width applies.
 */
@Composable
fun ExpressiveDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.width(getDialogWidth()),
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = SettingsShape.CornerExtraLarge1,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        properties = properties,
    )
}

/** Confirmation dialog sized and shaped like SPA's `SettingsAlertDialog`. */
@Composable
fun ExpressiveConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = stringResource(android.R.string.cancel),
    destructive: Boolean = false,
) {
    ExpressiveDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            ExpressiveTextButton(
                onClick = { onDismiss(); onConfirm() },
                colors = if (destructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                },
            ) { Text(confirmText) }
        },
        dismissButton = { ExpressiveTextButton(onClick = onDismiss) { Text(dismissText) } },
    )
}
