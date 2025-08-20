package com.sami.topscanner.componant

import android.R.attr.textColor
import android.R.style.Theme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sami.topscanner.R
import com.sami.topscanner.ui.theme.Primary

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = ImageVector.vectorResource(R.drawable.search)
) {
    BasicTextField(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x1A1C1C1C)),
        value = value,
        onValueChange = onValueChange,
        maxLines = 1,
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = Color.Black.copy(0.8f),
            lineHeight = 22.sp,
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(leadingIcon != null) {
                    Icon(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp),
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = Primary
                    )
                }
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    innerTextField()

                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF888888)
                            )
                        )
                    }
                }
            }
        }
    )
}