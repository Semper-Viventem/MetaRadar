package f.cking.software.ui.filter

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import f.cking.software.R
import f.cking.software.domain.model.RadarProfile
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Router

object SelectFilterScreen {

    @Composable
    fun Screen(
        initialFilterState: FilterUiState,
        router: Router,
        onConfirm: (filterState: RadarProfile.Filter) -> Unit
    ) {
        Scaffold(
            topBar = { AppBar { router.navigate(BackCommand) } },
            content = { paddings ->
                Column(modifier = Modifier.background(MaterialTheme.colors.surface).padding(paddings)) {
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                FilterScreen.Filter(
                                    filterState = initialFilterState,
                                    router = router,
                                    onDeleteClick = { router.navigate(BackCommand) }
                                )
                            }
                        }
                    }

                    Surface(elevation = 12.dp) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colors.primary)
                                .fillMaxWidth(),
                        ) {
                            val context = LocalContext.current
                            Button(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primaryVariant),
                                onClick = {
                                    val filter = initialFilterState
                                        .takeIf { it.isCorrect() }
                                        ?.let { FilterUiMapper.mapToDomain(it) }

                                    if (filter != null) {
                                        router.navigate(BackCommand)
                                        onConfirm.invoke(filter)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.filter_is_not_valid), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(text = stringResource(R.string.confirm), color = MaterialTheme.colors.onPrimary)
                            }
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun AppBar(onBackClick: () -> Unit) {
        TopAppBar(
            title = {
                Text(text = stringResource(R.string.create_filter))
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
    }
}