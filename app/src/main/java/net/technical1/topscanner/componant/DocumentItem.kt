package net.technical1.topscanner.componant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.technical1.topscanner.R
import net.technical1.topscanner.TimeUtil
import net.technical1.topscanner.model.DocumentFormat
import net.technical1.topscanner.model.ScannedDocument

@Composable
fun DocumentItem(
    document: ScannedDocument,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            imageVector = ImageVector.vectorResource(
                when (document.type) {
                    DocumentFormat.PDF -> R.drawable.pdf
                    DocumentFormat.JPEG -> R.drawable.jpeg
                    DocumentFormat.PNG -> R.drawable.png
                    DocumentFormat.DOCX -> R.drawable.docx
                }
            ),
            contentDescription = null,
            tint = Color.Unspecified
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = document.name,
                style = TextStyle(
                    color = Color.Black.copy(0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            )
            Text(
                text = stringResource(
                    R.string.last_modified_on,
                    TimeUtil.formatMillisToDisplay(document.lastModified)
                ),
                style = TextStyle(
                    color = Color.Black.copy(0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}