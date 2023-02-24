package f.cking.software.ui.selectfiltertype

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import org.koin.androidx.compose.koinViewModel

object SelectFilterTypeScreen {

    @Composable
    fun Screen(
        onSelected: (type: FilterType) -> Unit
    ) {
        val viewModel: SelectFilterTypeViewModel = koinViewModel()
        Scaffold(
            topBar = { AppBar(viewModel) },
            content = { paddings ->
                LazyColumn(modifier = Modifier.padding(paddings)) {
                    viewModel.types.forEach { type ->
                        item {
                            TypeItem(item = type) {
                                viewModel.back()
                                onSelected.invoke(type)
                            }
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun AppBar(viewModel: SelectFilterTypeViewModel) {
        TopAppBar(
            title = {
                Text(text = stringResource(R.string.select_filter))
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
    }

    @Composable
    private fun TypeItem(item: FilterType, onClickListener: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClickListener.invoke() }
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = stringResource(item.displayNameRes),
                fontSize = 18.sp
            )
        }
    }
}