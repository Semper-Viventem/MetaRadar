package f.cking.software.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import f.cking.software.common.Divider
import org.koin.androidx.compose.koinViewModel

object JournalScreen {

    @Composable
    fun Screen() {
        val viewModel: JournalViewModel = koinViewModel()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {

            viewModel.journal.map {
                item { JournalEntry(uiModel = it, viewModel) }
                item { Divider() }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
    @Composable
    fun JournalEntry(uiModel: JournalViewModel.JournalEntryUiModel, viewModel: JournalViewModel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = uiModel.color))
                .clickable { viewModel.onEntryClick(uiModel.journalEntry) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(modifier = Modifier.weight(1f), text = uiModel.title, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = uiModel.dateTime, fontWeight = FontWeight.Thin)
                }
                Spacer(modifier = Modifier.height(4.dp))
                var isExpanded by remember { mutableStateOf(false) }

                uiModel.subtitle?.let { subtitle ->
                    Text(
                        modifier = Modifier.clickable {
                            isExpanded = !isExpanded
                        },
                        text = uiModel.subtitle,
                        fontWeight = FontWeight.Normal,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 5
                    )
                }

                uiModel.items?.takeIf { it.isNotEmpty() }?.let { items ->
                    FlowRow(
                        mainAxisSpacing = 8.dp,
                    ) {
                        items.forEach { item ->
                            Chip(
                                onClick = { viewModel.onJournalListItemClick(item.payload) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = Color.LightGray,
                                    contentColor = Color.Black,
                                    leadingIconContentColor = Color.Black
                                ),
                            ) {
                                Text(text = item.displayName)
                            }
                        }
                    }
                }
            }
        }
    }
}