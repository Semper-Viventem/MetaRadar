package f.cking.software.ui.profileslist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                Text(text = "Create new")
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
                Text(text = profile.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                profile.description?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val activeText = if (profile.isActive) "Is active" else "Is NOT active"
                Text(text = activeText, fontWeight = FontWeight.Light)
            }
        }
    }
}