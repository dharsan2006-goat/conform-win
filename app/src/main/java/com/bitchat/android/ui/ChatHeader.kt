package com.bitchat.android.ui


import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import com.bitchat.android.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.core.ui.utils.singleOrTripleClickable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Header components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */




@Composable
fun TorStatusDot(
    modifier: Modifier = Modifier
) {
    val torProvider = remember { com.bitchat.android.net.ArtiTorManager.getInstance() }
    val torStatus by torProvider.statusFlow.collectAsState()
    
    if (torStatus.mode != com.bitchat.android.net.TorMode.OFF) {
        val dotColor = when {
            torStatus.running && torStatus.bootstrapPercent < 100 -> Color(0xFFFF9500) // Orange - bootstrapping
            torStatus.running && torStatus.bootstrapPercent >= 100 -> Color(0xFF00C851) // Green - connected
            else -> Color.Red // Red - error/disconnected
        }
        Canvas(
            modifier = modifier
        ) {
            val radius = size.minDimension / 2
            drawCircle(
                color = dotColor,
                radius = radius,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
    }
}

@Composable
fun NoiseSessionIcon(
    sessionState: String?,
    modifier: Modifier = Modifier
) {
    val (icon, color, contentDescription) = when (sessionState) {
        "uninitialized" -> Triple(
            Icons.Outlined.NoEncryption,
            Color(0x87878700), // Grey - ready to establish
            stringResource(R.string.cd_ready_for_handshake)
        )
        "handshaking" -> Triple(
            Icons.Outlined.Sync,
            Color(0x87878700), // Grey - in progress
            stringResource(R.string.cd_handshake_in_progress)
        )
        "established" -> Triple(
            Icons.Filled.Lock,
            Color(0xFFFF9500), // Orange - secure
            stringResource(R.string.cd_encrypted)
        )
        else -> { // "failed" or any other state
            Triple(
                Icons.Outlined.Warning,
                Color(0xFFFF4444), // Red - error
                stringResource(R.string.cd_handshake_failed)
            )
        }
    }
    
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = color
    )
}

@Composable
fun NicknameEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    
    // Auto-scroll to end when text changes (simulates cursor following)
    LaunchedEffect(value) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.at_symbol),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.primary.copy(alpha = 0.8f)
        )
        
        // BITCHAT MOD: Nickname is now immutable in the UI. 
        // Replaced BasicTextField with a read-only Text component.
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colorScheme.primary,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier
                .widthIn(max = 120.dp)
                .horizontalScroll(scrollState)
        )
    }
}

@Composable
fun PeerCounter(
    connectedPeers: List<String>,
    joinedChannels: Set<String>,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Mesh channel: show Bluetooth-connected peers (excluding self)
    val count = connectedPeers.size
    val meshBlue = Color(0xFF007AFF) // iOS-style blue for mesh
    val countColor = if (isConnected && count > 0) meshBlue else Color.Gray
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onClick() }.padding(end = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = stringResource(R.string.cd_connected_peers),
            modifier = Modifier.size(16.dp),
            tint = countColor
        )
        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium,
            color = countColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        if (joinedChannels.isNotEmpty()) {
            Text(
                text = stringResource(R.string.channel_count_prefix) + "${joinedChannels.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isConnected) Color(0xFF00C851) else Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ChatHeaderContent(
    nickname: String,
    viewModel: ChatViewModel,
    onBackClick: () -> Unit,
    onSidebarClick: () -> Unit,
    onTripleClick: () -> Unit,
    onShowAppInfo: () -> Unit
) {
    val currentChannel by viewModel.currentChannel.collectAsStateWithLifecycle()

    when {
        currentChannel != null -> {
            // Channel header
            ChannelHeader(
                channel = currentChannel!!,
                onBackClick = onBackClick,
                onLeaveChannel = { viewModel.leaveChannel(currentChannel!!) },
                onSidebarClick = onSidebarClick
            )
        }
        else -> {
            // Main header
            MainHeader(
                nickname = nickname,
                onTitleClick = onShowAppInfo,
                onTripleTitleClick = onTripleClick,
                onSidebarClick = onSidebarClick,
                viewModel = viewModel
            )
        }
    }
}



@Composable
private fun ChannelHeader(
    channel: String,
    onBackClick: () -> Unit,
    onLeaveChannel: () -> Unit,
    onSidebarClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Box(modifier = Modifier.fillMaxWidth()) {
        // Back button - positioned all the way to the left with minimal margin
        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-8).dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.chat_back),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.primary
                )
            }
        }
        
        // Title - perfectly centered regardless of other elements
        Text(
            text = stringResource(R.string.chat_channel_prefix, channel),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFFF9500), // Orange to match input field
            modifier = Modifier
                .align(Alignment.Center)
                .clickable { onSidebarClick() }
        )
        
        // Leave button - positioned on the right
        TextButton(
            onClick = onLeaveChannel,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                text = stringResource(R.string.chat_leave),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red
            )
        }
    }
}

@Composable
private fun MainHeader(
    nickname: String,
    onTitleClick: () -> Unit,
    onTripleTitleClick: () -> Unit,
    onSidebarClick: () -> Unit,
    viewModel: ChatViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val joinedChannels by viewModel.joinedChannels.collectAsStateWithLifecycle()
    val hasUnreadPrivateMessages by viewModel.unreadPrivateMessages.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_brand),
                style = MaterialTheme.typography.headlineSmall,
                color = colorScheme.primary,
                modifier = Modifier.singleOrTripleClickable(
                    onSingleClick = onTitleClick,
                    onTripleClick = onTripleTitleClick
                )
            )
            
            Spacer(modifier = Modifier.width(2.dp))
            
            NicknameEditor(
                value = nickname,
                onValueChange = {}
            )
        }
        
        // Right section with peer counter
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            // Unread private messages badge (click to open most recent DM)
            if (hasUnreadPrivateMessages.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = stringResource(R.string.cd_unread_private_messages),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { viewModel.openLatestUnreadPrivateChat() },
                    tint = Color(0xFFFF9500)
                )
            }

            // Tor status dot when Tor is enabled
            TorStatusDot(
                modifier = Modifier
                    .size(8.dp)
                    .padding(start = 0.dp, end = 2.dp)
            )
            
            // PoW status indicator
            PoWStatusIndicator(
                modifier = Modifier,
                style = PoWIndicatorStyle.COMPACT
            )
            Spacer(modifier = Modifier.width(2.dp))
            PeerCounter(
                connectedPeers = connectedPeers.filter { it != viewModel.meshService.myPeerID },
                joinedChannels = joinedChannels,
                isConnected = isConnected,
                onClick = onSidebarClick
            )
        }
    }
}
