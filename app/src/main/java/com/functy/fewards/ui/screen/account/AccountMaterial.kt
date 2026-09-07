package com.functy.fewards.ui.screen.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.functy.fewards.R
import com.functy.fewards.ui.component.material.TonalCard
import com.functy.fewards.ui.component.material.expressiveTopAppBarColors
import com.functy.fewards.ui.viewmodel.AccountViewModel

@Composable
fun AccountPagerMaterial(
    accountViewModel: AccountViewModel,
    bottomInnerPadding: androidx.compose.ui.unit.Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val state by accountViewModel.uiState.collectAsStateWithLifecycle()
    val actions = accountViewModel.accountActions

    Column(
        modifier = Modifier
            .padding(bottom = bottomInnerPadding)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        TopBar(scrollBehavior)
        // 米游社
        TonalCard {
            Column(modifier = Modifier.padding(16.dp, 12.dp)) {
                Text(
                    stringResource(R.string.miyoushe),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    stringResource(
                        if (state.mihoyoLoggedIn) R.string.miyoushe_status_logged_in
                        else R.string.miyoushe_status_not_logged_in
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.loginMode == 0,
                        onClick = { actions.onSetLoginMode(0) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text(stringResource(R.string.miyoushe_login_qr)) }
                    SegmentedButton(
                        selected = state.loginMode == 1,
                        onClick = { actions.onSetLoginMode(1) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text(stringResource(R.string.miyoushe_login_cookie)) }
                }
                Spacer(Modifier.height(12.dp))
                if (state.loginMode == 0) {
                    Button(onClick = actions.onStartQr) {
                        Text(
                            stringResource(
                                when (state.qrState) {
                                    AccountUiState.QrState.Waiting -> R.string.miyoushe_qr_wait_scan
                                    AccountUiState.QrState.Scanned -> R.string.miyoushe_qr_scanned
                                    AccountUiState.QrState.Confirmed -> R.string.miyoushe_qr_confirmed
                                    AccountUiState.QrState.Expired -> R.string.miyoushe_qr_refresh
                                    else -> R.string.miyoushe_qr_generate
                                }
                            )
                        )
                    }
                } else {
                    var cookie by rememberSaveable { mutableStateOf("") }
                    OutlinedTextField(
                        value = cookie,
                        onValueChange = { cookie = it },
                        label = { Text(stringResource(R.string.miyoushe_cookie_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (cookie.contains("stoken")) {
                                    actions.onImportCookie(cookie)
                                    cookie = ""
                                }
                            }
                        ) { Text(stringResource(R.string.miyoushe_cookie_import)) }
                    }
                }
            }
        }
        state.mihoyoAccounts.forEach { account ->
            TonalCard {
                ListItem(
                    headlineContent = { Text(account.nickname) },
                    supportingContent = { Text("stuid=${account.stuid}") },
                    leadingContent = { Icon(Icons.Filled.Person, null) },
                    trailingContent = {
                        androidx.compose.material3.IconButton(onClick = { actions.onRemoveMihoyo(account.id) }) {
                            Icon(Icons.Filled.Delete, null, Modifier.padding(4.dp))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
        // WorkBuddy
        TonalCard {
            Column(modifier = Modifier.padding(16.dp, 12.dp)) {
                Text(
                    stringResource(R.string.workbuddy),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    stringResource(
                        if (state.wbLoggedIn) R.string.workbuddy_status_logged_in
                        else R.string.workbuddy_status_not_logged_in
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                var wbToken by rememberSaveable { mutableStateOf("") }
                OutlinedTextField(
                    value = wbToken,
                    onValueChange = { wbToken = it },
                    label = { Text(stringResource(R.string.workbuddy_token_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            actions.onImportWbToken(wbToken)
                            wbToken = ""
                        }
                    ) { Text(stringResource(R.string.workbuddy_token_import)) }
                }
            }
        }
        state.wbAccounts.forEach { account ->
            TonalCard {
                ListItem(
                    headlineContent = { Text(account.label) },
                    supportingContent = { Text("token=${account.token.take(6)}****") },
                    leadingContent = { Icon(Icons.Filled.Person, null) },
                    trailingContent = {
                        androidx.compose.material3.IconButton(onClick = { actions.onRemoveWb(account.id) }) {
                            Icon(Icons.Filled.Delete, null, Modifier.padding(4.dp))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TopBar(scrollBehavior: TopAppBarScrollBehavior? = null) {
    LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.account_title)) },
        colors = expressiveTopAppBarColors(),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}
