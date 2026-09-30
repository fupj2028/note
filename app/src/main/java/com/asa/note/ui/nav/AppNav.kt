package com.asa.note.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.asa.note.AppContainer
import com.asa.note.repo.AppSettings
import com.asa.note.ui.book.BookManageScreen
import com.asa.note.ui.category.CategoryManageScreen
import com.asa.note.ui.editor.NoteEditorScreen
import com.asa.note.ui.excerpt.ExcerptComposerScreen
import com.asa.note.ui.excerpt.ExcerptDetailScreen
import com.asa.note.ui.excerpt.ExcerptStreamScreen
import com.asa.note.ui.list.NoteListScreen
import com.asa.note.ui.search.SearchScreen
import com.asa.note.ui.settings.SettingsScreen
import com.asa.note.ui.trash.TrashScreen

private object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
    const val EXCERPT_DETAIL = "excerpt"
    const val EXCERPT_COMPOSER = "excerptComposer"
    const val SEARCH = "search"
    const val TRASH = "trash"
    const val CATEGORIES = "categories"
    const val BOOKS = "books"
}

/** 底栏内容净高。系统手势区留白另外由 windowInsetsPadding 加，不混在这个数里。 */
private val BottomBarContentHeight = 60.dp

@Composable
fun AppNav(container: AppContainer, settings: AppSettings) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                onOpenEditor = { noteId -> nav.navigate("${Routes.EDITOR}?noteId=$noteId") },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenExcerptDetail = { excerptId -> nav.navigate("${Routes.EXCERPT_DETAIL}/$excerptId") },
                onOpenExcerptComposer = { source ->
                    nav.navigate("${Routes.EXCERPT_COMPOSER}?source=${Uri.encode(source.orEmpty())}")
                },
                onOpenSearch = { nav.navigate(Routes.SEARCH) },
                onOpenCategories = { nav.navigate(Routes.CATEGORIES) },
                onOpenBookManage = { nav.navigate(Routes.BOOKS) },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onOpenNote = { noteId -> nav.navigate("${Routes.EDITOR}?noteId=$noteId") },
                onOpenExcerpt = { excerptId -> nav.navigate("${Routes.EXCERPT_DETAIL}/$excerptId") },
            )
        }

        composable(
            route = "${Routes.EDITOR}?noteId={noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
        ) { entry ->
            NoteEditorScreen(
                container = container,
                noteId = entry.arguments?.getLong("noteId") ?: 0L,
                onBack = { nav.popBackStack() },
            )
        }

        composable(
            route = "${Routes.EXCERPT_DETAIL}/{excerptId}",
            arguments = listOf(navArgument("excerptId") { type = NavType.LongType }),
        ) { entry ->
            ExcerptDetailScreen(
                container = container,
                excerptId = entry.arguments?.getLong("excerptId") ?: 0L,
                onBack = { nav.popBackStack() },
            )
        }

        composable(
            route = "${Routes.EXCERPT_COMPOSER}?source={source}",
            arguments = listOf(
                navArgument("source") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            ExcerptComposerScreen(
                container = container,
                // 在某本书的筛选下点加号，出处直接填这本书名（可改）；「全部」下为空。
                initialSource = entry.arguments?.getString("source").orEmpty(),
                onDone = { nav.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                settings = settings,
                onBack = { nav.popBackStack() },
                onOpenTrash = { nav.navigate(Routes.TRASH) },
                onOpenCategories = { nav.navigate(Routes.CATEGORIES) },
                onOpenBooks = { nav.navigate(Routes.BOOKS) },
            )
        }

        composable(Routes.CATEGORIES) {
            CategoryManageScreen(
                container = container,
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.BOOKS) {
            BookManageScreen(
                container = container,
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.TRASH) {
            TrashScreen(
                container = container,
                onBack = { nav.popBackStack() },
            )
        }
    }
}

@Composable
private fun HomeScreen(
    container: AppContainer,
    onOpenEditor: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExcerptDetail: (Long) -> Unit,
    onOpenExcerptComposer: (String?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBookManage: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        // 外层清零系统栏内边距：底部留白由 BottomBar 自己的高度给，顶部留给内层页面去要。
        // 两边都算一遍会让顶部白空出整整一个状态栏的高度。
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            BottomBar(selected = tab, onSelect = { tab = it })
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (tab) {
                0 -> NoteListScreen(
                    container = container,
                    onOpenEditor = onOpenEditor,
                    onOpenSettings = onOpenSettings,
                    onOpenSearch = onOpenSearch,
                    onOpenCategories = onOpenCategories,
                )
                else -> ExcerptStreamScreen(
                    container = container,
                    onOpenDetail = onOpenExcerptDetail,
                    onOpenComposer = onOpenExcerptComposer,
                    onOpenSearch = onOpenSearch,
                    onOpenBookManage = onOpenBookManage,
                )
            }
        }
    }
}

/**
 * 不用 Material 的 NavigationBar：它把系统手势区的留白算进总高度里，
 * 想压矮就得连手势区一起压，结果标签被挤到手势条底下、底色也铺不到屏幕底。
 * 自己画的话，净高和手势留白是两件事，互不牵连。
 */
@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(BottomBarContentHeight),
            ) {
                BottomTab(
                    label = "备忘录",
                    icon = Icons.Filled.Edit,
                    selected = selected == 0,
                    onClick = { onSelect(0) },
                )
                BottomTab(
                    label = "摘录",
                    icon = Icons.AutoMirrored.Filled.List,
                    selected = selected == 1,
                    onClick = { onSelect(1) },
                )
            }
            // 手势区留白单独占一段，仍在 Surface 内 —— 这样底色一直铺到屏幕底，
            // 不会出现"栏是米色、手势条那截是白色"的断层。
            Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun RowScope.BottomTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        Color.Transparent
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
