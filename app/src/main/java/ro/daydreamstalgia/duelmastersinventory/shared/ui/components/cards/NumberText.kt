package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.Fonts

@Composable
fun NumberText(
    number:String,
    modifier: Modifier = Modifier,
    strokeWidth:Float=4f
) {
    Box(modifier=modifier){
        Text(
            text = number, fontFamily = Fonts.numbersFamily, fontSize = 28.sp,
            color = Color.White,
            style = TextStyle.Default.copy(
                fontSize = 28.sp,
            )
        )
        Text(
            text = number, fontFamily = Fonts.numbersFamily, fontSize = 28.sp,
            color = Color.Black,
            style = TextStyle.Default.copy(
                fontSize = 28.sp,
                drawStyle = Stroke(
                    miter = 10f,
                    width = strokeWidth,
                    join = StrokeJoin.Bevel
                ),
                brush = SolidColor(Color.Red)
            )
        )
    }
}