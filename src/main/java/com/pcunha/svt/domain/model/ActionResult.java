package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.ActionOutcome;

public record ActionResult(ActionOutcome outcome, ActionRoll chosenRoll) {
}
