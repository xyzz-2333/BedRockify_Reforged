"""Real screen-lifecycle regression checks for the alpha.6 recipe-panel repair.

Start a disposable Forge client world with the survival fixture and JEI, then
run this script with run/client. Commands exercise native screen and input paths.
"""
import json
import sys
import time
import uuid
from pathlib import Path

root = Path(sys.argv[1]).resolve()
records = []


def send(action, **fields):
    command = {"action": action, "request_id": uuid.uuid4().hex, **fields}
    result = root / "survival-qa-result.json"
    result.unlink(missing_ok=True)
    temp = root / "survival-qa-command.tmp"
    temp.write_text(json.dumps(command), encoding="utf-8")
    temp.replace(root / "survival-qa-command.json")
    deadline = time.monotonic() + 40
    while time.monotonic() < deadline:
        try:
            value = json.loads(result.read_text(encoding="utf-8"))
        except (FileNotFoundError, json.JSONDecodeError):
            time.sleep(.05)
            continue
        if value.get("request_id") != command["request_id"]:
            time.sleep(.05)
            continue
        records.append({"command": command, "result": value})
        if not value.get("ok"):
            raise AssertionError(value)
        return value
    raise TimeoutError(command)


def until(predicate):
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        value = send("status")
        if predicate(value):
            return value
        time.sleep(.1)
    raise AssertionError(value)


def open_with_preference():
    value = send("inventory")
    if not value["book_open"]:
        value = send("user_toggle_book")
    assert value["book_open"] and value["recipe_open_preference"]
    return value


def report(name):
    print("PASS " + name, flush=True)


try:
    send("setup")
    until(lambda value: value["hotbar"][0] == "64 oak_log")
    state = open_with_preference()
    assert state["classic"] and state["recovery_toggle"]

    state = send("user_toggle_book")
    assert not state["book_open"] and not state["recipe_open_preference"]
    state = send("inventory")
    assert not state["book_open"] and state["recovery_toggle"]
    state = send("user_toggle_book")
    assert state["book_open"] and state["recipe_open_preference"]
    report("deliberate close/open persists, with a usable recovery control")

    state = send("transient_close")
    assert not state["book_open"] and state["recipe_open_preference"]
    state = send("inventory")
    assert state["book_open"] and state["classic"]
    report("external temporary closure does not overwrite the user's open preference")

    state = send("remove_recipe_controls")
    assert not any("RecipeBookToggle" in button["class"] for button in state["buttons"])
    state = send("user_toggle_book")
    assert not state["book_open"]
    state = send("user_toggle_book")
    assert state["book_open"] and state["recipe_open_preference"]
    report("recipe control still responds after native and custom widgets are removed")

    state = send("redisplay", close_book=True)
    assert state["classic"] and state["book_open"] and state["background_width"] == 218
    report("native temporary-screen redisplay restores layout and preferred open state")

    send("reuse_without_init")
    state = until(lambda value: value.get("classic") and value.get("book_open"))
    assert state["background_width"] == 218 and state["slots"][0]["x"] == 182
    report("screen wrapper return without normal initialization recovers on rendering")

    state = send("disable_redisplay")
    assert not state["classic"] and state["background_width"] == 176 and state["background_height"] == 166
    assert state["slots"][0]["x"] == 154
    send("toggle_setting", enabled=True)
    state = open_with_preference()
    report("redisplay into native fallback restores background and original slot positions")

    state = send("jei_recipes")
    assert state["screen"].endswith("RecipesGui")
    state = send("return_screen")
    assert state["screen"].endswith("InventoryScreen") and state["classic"] and state["book_open"]
    assert state["slots"][0]["x"] == 182
    report("real JEI recipe screen returns to the same inventory with the recipe panel")

    send("crafting")
    state = until(lambda value: value["screen"].endswith("CraftingScreen"))
    if not state["book_open"]:
        state = send("user_toggle_book")
    assert state["classic"] and state["recovery_toggle"]
    state = send("transient_close")
    assert not state["book_open"] and state["recipe_open_preference"]
    state = send("user_toggle_book")
    assert state["book_open"]
    send("remove_recipe_controls")
    state = send("user_toggle_book")
    assert not state["book_open"]
    state = send("user_toggle_book")
    assert state["book_open"]
    send("jei_recipes")
    state = send("return_screen")
    assert state["screen"].endswith("CraftingScreen") and state["classic"] and state["book_open"]
    assert state["slots"][0]["x"] == 164
    report("workbench recovery, removed-widget input and real JEI round trip")

    state = send("disable_redisplay")
    assert not state["classic"] and state["background_width"] == 176 and state["background_height"] == 166
    assert state["slots"][0]["x"] == 124
    send("toggle_setting", enabled=True)
    send("close")
    open_with_preference()
    report("workbench redisplay into native fallback restores its own geometry")
finally:
    (root / "survival-recovery-validation.json").write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")
