package f.cking.software.ui.profileslist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import f.cking.software.domain.model.RadarProfile
import org.koin.androidx.compose.koinViewModel

object ProfilesListScreen {

    @Composable
    fun Screen() {
        val viewModel: ProfilesListViewModel = koinViewModel()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {

            item { CreateNewButton(viewModel = viewModel) }
            viewModel.profiles.map { item { ListItem(profile = it, viewModel = viewModel) } }
        }
    }

    @Composable
    private fun CreateNewButton(viewModel: ProfilesListViewModel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(onClick = { viewModel.createNewClick() }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.create_new))
            }
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
                Text(text = profile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))
                val activeText = if (profile.isActive) stringResource(R.string.profile_is_active) else stringResource(R.string.profile_is_not_active)
                val color = if (profile.isActive) colorResource(id = R.color.green_600) else Color.DarkGray
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
                    Text(text = description)
                }
            }
        }
    }
}