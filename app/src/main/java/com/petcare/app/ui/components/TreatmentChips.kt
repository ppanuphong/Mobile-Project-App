package com.petcare.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.petcare.app.model.PetCondition
import com.petcare.app.model.TreatmentOutcome
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.ui.theme.StatusColors

@Composable
fun TreatmentOutcome.colors(): StatusColors {
    val c = PetCareTheme.colors
    return when (this) {
        TreatmentOutcome.ONGOING -> StatusColors(c.amberContainer, c.onAmberContainer, c.amber, c.amberText)
        TreatmentOutcome.RECOVERED -> StatusColors(c.doneContainer, c.onDoneContainer, c.done, c.done)
        TreatmentOutcome.NORMAL -> MaterialTheme.colorScheme.let {
            StatusColors(it.surfaceVariant, it.onSurfaceVariant, it.outline, it.onSurfaceVariant)
        }
    }
}

@Composable
fun PetCondition.colors(): StatusColors {
    val c = PetCareTheme.colors
    return when (this) {
        PetCondition.BETTER -> StatusColors(c.doneContainer, c.onDoneContainer, c.done, c.done)
        PetCondition.RECOVERED -> StatusColors(c.upcomingContainer, c.onUpcomingContainer, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
        PetCondition.STABLE -> StatusColors(c.amberContainer, c.onAmberContainer, c.amber, c.amberText)
        PetCondition.WORSE -> StatusColors(c.rustContainer, c.onRustContainer, c.rust, c.rust)
    }
}

@Composable
private fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
    }
}

@Composable
fun OutcomeChip(outcome: TreatmentOutcome, modifier: Modifier = Modifier) {
    val colors = outcome.colors()
    Pill(outcome.label, colors.container, colors.content, modifier)
}

@Composable
fun ConditionChip(condition: PetCondition, modifier: Modifier = Modifier) {
    val colors = condition.colors()
    Pill(condition.label, colors.container, colors.content, modifier)
}
