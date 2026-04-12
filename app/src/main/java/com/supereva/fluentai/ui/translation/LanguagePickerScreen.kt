package com.supereva.fluentai.ui.translation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerScreen(
    isSource: Boolean,
    currentSelectedLanguage: String,
    onBack: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    disabledLanguage: String = ""
) {
    val darkBackground = Color(0xFF0D0D0D)
    val surfaceColor = Color.White.copy(alpha = 0.05f)
    val brandGreen = Color(0xFF4CAF50)
    val selectedBgColor = Color(0xFF1E3A8A).copy(alpha = 0.3f) // Light blueish dark theme variant
    val defaultText = Color.White
    
    var searchQuery by remember { mutableStateOf("") }
    
    // Hardcoded language lists for demonstration
    val recentLanguages = remember { listOf("English", "Hindi") }
    val allLanguages = remember {
        listOf("English", "French", "German", "Hindi", "Spanish").sorted()
    }
    
    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allLanguages
        } else {
            allLanguages.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = darkBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isSource) "Translate from" else "Translate to",
                        color = defaultText
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = defaultText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search languages", color = Color.White.copy(alpha = 0.4f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.4f))
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = defaultText,
                        unfocusedTextColor = defaultText,
                        cursorColor = brandGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {

                if (searchQuery.isBlank()) {
                    item {
                        Text(
                            text = "Recent Languages",
                            color = brandGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                        )
                    }
                    
                    items(recentLanguages) { language ->
                        LanguageRow(
                            name = language,
                            isSelected = currentSelectedLanguage == language,
                            selectedBgColor = selectedBgColor,
                            defaultText = defaultText,
                            onClick = { onLanguageSelected(language) },
                            isDisabled = language == disabledLanguage
                        )
                    }

                    item {
                        Text(
                            text = "All Languages",
                            color = brandGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                        )
                    }
                }
                
                items(filteredLanguages) { language ->
                    LanguageRow(
                        name = language,
                        isSelected = currentSelectedLanguage == language,
                        selectedBgColor = selectedBgColor,
                        defaultText = defaultText,
                        onClick = { onLanguageSelected(language) },
                        isDisabled = language == disabledLanguage
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(
    name: String,
    isSelected: Boolean,
    selectedBgColor: Color,
    defaultText: Color,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isDisabled: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDisabled, onClick = onClick)
            .background(if (isSelected) selectedBgColor else Color.Transparent)
            .alpha(if (isDisabled) 0.3f else 1.0f)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50), // Brand Green
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = name,
                color = defaultText,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = defaultText,
                    modifier = Modifier
                        .size(20.dp)
                )
            }
        }
    }
}
