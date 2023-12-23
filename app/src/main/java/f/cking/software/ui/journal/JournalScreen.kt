package f.cking.software.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import f.cking.software.R
import f.cking.software.utils.graphic.ContentPlaceholder
import f.cking.software.utils.graphic.Divider
import f.cking.software.utils.graphic.FABSpacer
import org.koin.androidx.compose.koinViewModel

object JournalScreen {

    @Composable
    fun Screen() {
        val viewModel: JournalViewModel = koinViewModel()
        val journal = viewModel.journal
        val modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
        if (journal.isEmpty()) {
            ContentPlaceholder(text = stringResource(R.string.journal_placeholder), modifier = modifier)
        } else {
            LazyColumn(
                modifier = modifier
            ) {
                journal.map {
                    item { JournalEntry(uiModel = it, viewModel) }
                    item { Divider() }
                }
                item { FABSpacer() }
            }
        }
    }

    @Composable
    fun JournalEntry(uiModel: JournalViewModel.JournalEntryUiModel, viewModel: JournalViewModel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(uiModel.color())
                .clickable { viewModel.onEntryClick(uiModel.journalEntry) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = uiModel.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        color = uiModel.colorForeground()
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = uiModel.dateTime, fontWeight = FontWeight.Thin, color = uiModel.colorForeground())
                }
                Spacer(modifier = Modifier.height(4.dp))
                var isExpanded by remember { mutableStateOf(false) }

                uiModel.subtitle?.let { subtitle ->
                    Text(
                        modifier = Modifier.clickable {
                            isExpanded = !isExpanded
                        },
                        text = if (isExpanded) uiModel.subtitle else uiModel.subtitleCollapsed.orEmpty(),
                        fontWeight = FontWeight.Normal,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis,
                        color = uiModel.colorForeground()
                    )
                }

                uiModel.items?.takeIf { it.isNotEmpty() }?.let { items ->
                    FlowRow(
                        mainAxisSpacing = 8.dp,
                    ) {
                        items.forEach { item ->
                            SuggestionChip(
                                onClick = { viewModel.onJournalListItemClick(item.payload) },
                                label = { Text(text = item.displayName) }
                            )
                        }
                    }
                }
            }
        }
    }
}