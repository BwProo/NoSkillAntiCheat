package ac.grim.grimac.checks.impl.inventory;

/**
 * NoSkillAntiCheat: pure per-tick logic of the QuickSwap check, unit-tested. Feed it the hotbar
 * slot changes and actions of one client tick in order, then call {@link #endTick()}.
 */
public final class SwapPattern {
    private int slotAtTickStart = -1;
    private int currentSlot = -1;
    private boolean changedThisTick, actionAfterChange;
    private int pendingReturnSlot = -1; // switched away and acted last tick, not back yet

    public void slotChanged(int slot) {
        if (currentSlot == -1) {
            currentSlot = slotAtTickStart = slot;
            return;
        }
        if (slot != currentSlot) changedThisTick = true;
        currentSlot = slot;
    }

    public void action() {
        if (changedThisTick) actionAfterChange = true;
    }

    /**
     * @return 2 for switch, act and switch back inside one tick (impossible by hand),
     * 1 for switching back one tick later, 0 otherwise
     */
    public int endTick() {
        int result = 0;
        if (pendingReturnSlot != -1 && currentSlot == pendingReturnSlot) {
            result = 1;
        } else if (changedThisTick && actionAfterChange && currentSlot == slotAtTickStart) {
            result = 2;
        }
        pendingReturnSlot = (result == 0 && changedThisTick && actionAfterChange && currentSlot != slotAtTickStart)
                ? slotAtTickStart : -1;
        slotAtTickStart = currentSlot;
        changedThisTick = actionAfterChange = false;
        return result;
    }
}
