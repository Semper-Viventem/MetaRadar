package f.cking.software.ui.profileslist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import f.cking.software.common.BottomSpacer
import f.cking.software.common.ContentPlaceholder
import f.cking.software.domain.model.RadarProfile
import org.koin.androidx.compose.koinViewModel

object ProfilesListScreen {

    @Composable
    fun Screen() {
        val viewModel: ProfilesListViewModel = koinViewModel()
        Column(
            Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Header(viewModel = viewModel)
            val profiles = viewModel.profiles
            if (profiles.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    profiles.map { item { ListItem(profile = it, viewModel = viewModel) } }
                    item {
                        BottomSpacer()
                    }
                }
            } else {
                ContentPlaceholder(stringResource(R.string.radar_profile_placeholder))
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun Header(viewModel: ProfilesListViewModel) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = 4.dp,
        ) {
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                item {
                    Spacer(modifier = Modifier.width(8.dp))
                    Chip(
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.primaryVariant,
                            contentColor = MaterialTheme.colors.onPrimary,
                        ),
                        onClick = { viewModel.createNewClick() },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        }
                    ) { Text(text = stringResource(R.string.create_new)) }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                viewModel.defaultFiltersTemplate.forEach {
                    item {
                        DefaultFilterChip(filter = it, viewModel = viewModel)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun DefaultFilterChip(filter: ProfilesListViewModel.FilterTemplate, viewModel: ProfilesListViewModel) {
        Chip(onClick = { viewModel.selectFilterTemplate(filter) }) {
            Text(text = stringResource(filter.displayNameRes))
        }
    }

    @Composable
    private fun ListItem(profile: RadarProfile, viewModel: ProfilesListViewModel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.onProfileClick(profile) }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = profile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onSurface)

                Spacer(modifier = Modifier.height(4.dp))
                val activeText = if (profile.isActive) stringResource(R.string.profile_is_active) else stringResource(R.string.profile_is_not_active)
                val color = if (profile.isActive) colorResource(id = R.color.green_600) else MaterialTheme.colors.onSurface
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier.size(12.dp),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_circle),
                        tint = color,
                        contentDescription = activeText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = activeText, fontWeight = FontWeight.Bold, color = color)
                }

                val description = profile.description
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = description, color = MaterialTheme.colors.onSurface)
                }
            }
        }
    }
}