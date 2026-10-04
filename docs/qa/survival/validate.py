"""Exercise actual client/server slot actions using the development fixture.

Run after loading a disposable world with the fixture, passing run/client.
The generated recipe/fixture JAR is deliberately excluded from releases.
"""
import argparse
import json
import time
import uuid
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("run_dir", type=Path)
parser.add_argument("--performance", action="store_true")
args = parser.parse_args()
root = args.run_dir.resolve()
records = []


def send(action, **fields):
    cmd = {"action": action, "request_id": uuid.uuid4().hex, **fields}
    result = root / "survival-qa-result.json"
    result.unlink(missing_ok=True)
    temp = root / "survival-qa-command.tmp"
    temp.write_text(json.dumps(cmd), encoding="utf-8")
    temp.replace(root / "survival-qa-command.json")
    deadline = time.monotonic() + 40
    while time.monotonic() < deadline:
        try:
            value = json.loads(result.read_text(encoding="utf-8"))
        except (FileNotFoundError, json.JSONDecodeError):
            time.sleep(.05)
            continue
        if value.get("request_id") != cmd["request_id"]:
            time.sleep(.05)
            continue
        records.append({"command": cmd, "result": value})
        if not value.get("ok"):
            raise AssertionError(value)
        return value
    raise TimeoutError(cmd)


def until(predicate):
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        state = send("status")
        if predicate(state):
            return state
        time.sleep(.1)
    raise AssertionError(state)


def slot(state, i):
    return state["slots"][i]["item"]


def click(i, button=0, type="PICKUP"):
    send("slot", id=i, button=button, type=type)


def fresh():
    send("close")
    send("toggle_setting", enabled=True)  # Establish the fixture's initial state explicitly.
    send("setup")
    time.sleep(.2)  # Wait for the integrated server and inventory packets.
    until(lambda s: s["hotbar"][0] == "64 oak_log")
    state = send("inventory")
    if not state["book_open"]:
        state = send("toggle_book")
    return state


def report(name):
    print("PASS " + name, flush=True)


try:
    state = fresh()
    assert state["classic"] and state["slot_count"] == 46
    send("search", text="minecraft:oak_planks")
    send("recipe", id="minecraft:oak_planks")
    state = until(lambda s: slot(s, 0) == "4 oak_planks" and sum(slot(s, i) == "1 oak_log" for i in range(1, 5)) == 1)
    click(0)
    state = until(lambda s: s["cursor"] == "4 oak_planks")
    assert slot(state, 0) == "0 air"
    click(9)
    until(lambda s: slot(s, 9) == "4 oak_planks" and s["cursor"] == "0 air")
    report("2x2 recipe fill, output acquisition and ingredient consumption")

    fresh()
    click(43, type="QUICK_MOVE")
    until(lambda s: slot(s, 6) == "1 leather_chestplate" and slot(s, 43) == "0 air")
    click(44)
    click(45)
    until(lambda s: slot(s, 45) == "1 shield" and s["cursor"] == "0 air")
    click(9, button=0, type="SWAP")
    until(lambda s: slot(s, 9) == "64 oak_log" and slot(s, 36) == "0 air")
    report("armor quick-move, offhand pickup and number-key slot swap")

    fresh()
    click(37)
    until(lambda s: s["cursor"] == "64 oak_planks")
    click(-999, button=0, type="QUICK_CRAFT")
    click(9, button=1, type="QUICK_CRAFT")
    click(10, button=1, type="QUICK_CRAFT")
    click(-999, button=2, type="QUICK_CRAFT")
    until(lambda s: slot(s, 9) == "32 oak_planks" and slot(s, 10) == "32 oak_planks" and s["cursor"] == "0 air")
    report("native drag distribution across relocated slots")

    fresh()
    state = send("search", text="#minecraft:planks")
    assert state["filtered_count"] >= 100 and not state["groups"]
    state = send("search", text="QA 木材0 木板")
    assert state["filtered_count"] == 1
    state = send("search", text="not_a_real_recipe_zzzz")
    assert state["entry_count"] == 0
    state = send("search", text="")
    before = state["matching_passes"]
    state = send("expand", enabled=True)
    assert state["entry_count"] >= 11049
    state = send("category", index=2)
    assert state["matching_passes"] == before
    send("category", index=0)
    send("expand", enabled=False)
    report("tag, Chinese and empty searches; expansion preserves recipe identities")

    send("search", text="bedrockifysurvivalqa:stress_")
    assert send("verify_variants", count=10000)["verified_variants"] == 10000
    send("search", text="bedrockifysurvivalqa:stress_123")
    send("recipe", id="bedrockifysurvivalqa:stress_123")
    state = until(lambda s: "qa_variant:" in s["slots"][0].get("nbt", ""))
    # Identical ingredients intentionally overlap; the server owns which
    # matching recipe wins. The result must retain the server's output NBT.
    result_nbt = state["slots"][0]["nbt"]
    click(0)
    until(lambda s: "metal_0_nugget" in s["cursor"])
    click(9)
    until(lambda s: s["slots"][9].get("nbt") == result_nbt)
    report("NBT result variant survives native recipe fill and output pickup")

    fresh()
    send("crafting")
    state = until(lambda s: s["screen"].endswith("CraftingScreen"))
    assert state["classic"] and state["slot_count"] == 46 and state["sync_id"] != 0
    send("search", text="minecraft:crafting_table")
    send("recipe", id="minecraft:crafting_table")
    state = until(lambda s: slot(s, 0) == "1 crafting_table" and sum(slot(s, i) == "1 oak_planks" for i in range(1, 10)) == 4)
    click(0, type="QUICK_MOVE")
    until(lambda s: any("crafting_table" in item["item"] for item in s["slots"][10:]))
    send("search", text="minecraft:oak_stairs")
    send("recipe", id="minecraft:oak_stairs")
    state = until(lambda s: slot(s, 0) == "4 oak_stairs" and sum(slot(s, i) == "1 oak_planks" for i in range(1, 10)) == 6)
    click(0)
    until(lambda s: s["cursor"] == "4 oak_stairs")
    click(10)
    until(lambda s: slot(s, 10) == "4 oak_stairs" and s["cursor"] == "0 air")
    report("real 3x3 workbench recipes and native shift output transfer")

    state = fresh()
    state = send("search", text="minecraft:oak_stairs")
    assert state["entry_count"] == 0
    state = send("resize", width=320, height=200)
    assert not state["classic"] and state["slots"][0]["x"] == 154
    state = send("resize", width=640, height=360)
    assert state["classic"] and state["slots"][0]["x"] == 182
    send("toggle_setting", enabled=False)
    state = send("inventory")
    assert not state["classic"] and state["slots"][0]["x"] == 154
    send("toggle_setting", enabled=True)
    state = send("inventory")
    assert state["classic"]
    report("2x2 fit filtering, small-window fallback and setting restore")

    if args.performance:
        for action, options in [("stress_filters", {"cycles": 500}), ("bench_match", {}), ("stress_open", {"cycles": 100})]:
            value = send(action, **options)
            assert "elapsed_ms" in value if action == "stress_filters" else "median_ms" in value
            print(action + " " + json.dumps({k: v for k, v in value.items() if k.endswith("_ms") or k.startswith("heap_") or k in ("cycles", "matching_passes_added", "max_heap")}), flush=True)
        report("stress checks")
finally:
    (root / "survival-validation.json").write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")
