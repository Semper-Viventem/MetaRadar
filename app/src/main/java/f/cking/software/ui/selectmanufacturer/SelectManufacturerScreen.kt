package f.cking.software.ui.selectmanufacturer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.toHexString
import f.cking.software.utils.graphic.GlassSystemNavbar
import f.cking.software.utils.graphic.SystemNavbarSpacer
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
object SelectManufacturerScreen {

    @Composable
    fun Screen(
        onSelected: (type: ManufacturerInfo) -> Unit
    ) {
        val viewModel: SelectManufacturerViewModel = koinViewModel()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize(),
            topBar = { AppBar(viewModel, scrollBehavior) },
            content = { paddings ->
                GlassSystemNavbar(Modifier.fillMaxSize()) {
                    Content(Modifier.padding(top = paddings.calculateTopPadding()), viewModel, onSelected)
                }
            }
        )
    }

    @Composable
    private fun Content(modifier: Modifier, viewModel: SelectManufacturerViewModel, onSelected: (type: ManufacturerInfo) -> Unit) {
        LazyColumn(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize()
        ) {
            val list = viewModel.manufacturers
            list.forEach { type ->
                item {
                    TypeItem(item = type) {
                        onSelected.invoke(type)
                        viewModel.back()
                    }
                }
            }
            item { SystemNavbarSpacer() }
        }
    }

    @Composable
    private fun AppBar(viewModel: SelectManufacturerViewModel, scrollBehavior: TopAppBarScrollBehavior) {
        TopAppBar(
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            title = {
                TextField(
                    maxLines = 1,
                    value = viewModel.searchStr,
                    onValueChange = { viewModel.searchRequest(it) },
                    placeholder = { Text(text = stringResource(R.string.select_manufacturer)) }
                )
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
    }

    @Composable
    private fun TypeItem(item: ManufacturerInfo, onClickListener: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClickListener.invoke() }
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = "${item.name} (0x${item.id.toHexString()})",
                fontSize = 18.sp,
            )
        }
    }
}