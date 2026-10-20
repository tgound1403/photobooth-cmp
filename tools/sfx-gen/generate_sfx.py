#!/usr/bin/env python3
"""Generates the three shutter sounds in app/src/main/assets/sounds/.

Stdlib only (wave, struct, math, random). Output is deterministic: the noise is seeded.
Run from the project root:  python3 -I tools/sfx-gen/generate_sfx.py

Each file is mono, 16-bit PCM, 44100 Hz, normalised to a peak of 0.6 of full scale.
"""

import math
import random
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 44100
PEAK = 0.6
SEED = 20260101

OUT_DIR = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "assets" / "sounds"


def _samples(duration: float) -> int:
    return int(round(duration * SAMPLE_RATE))


def _burst(rng: random.Random, start: float, length: float, out: list[float], amp: float) -> None:
    """Adds a short white-noise burst with a fast exponential decay, starting at [start] seconds."""
    i0 = int(round(start * SAMPLE_RATE))
    n = _samples(length)
    for k in range(n):
        idx = i0 + k
        if idx >= len(out):
            break
        t = k / SAMPLE_RATE
        env = math.exp(-t / (length / 4.0))
        out[idx] += amp * env * (rng.random() * 2.0 - 1.0)


def _tone(out: list[float], freq: float, decay: float, amp: float, duration: float) -> None:
    """Adds a sine tone with an exponential decay (time constant [decay] seconds)."""
    n = min(_samples(duration), len(out))
    for k in range(n):
        t = k / SAMPLE_RATE
        out[k] += amp * math.exp(-t / decay) * math.sin(2.0 * math.pi * freq * t)


def _lowpass(samples: list[float], alpha: float) -> list[float]:
    """One-pole low-pass filter. Smaller alpha means darker sound."""
    y = 0.0
    result = []
    for x in samples:
        y += alpha * (x - y)
        result.append(y)
    return result


def _fade_out(samples: list[float], seconds: float) -> None:
    """Applies a short linear fade to the last [seconds] so the file ends without a click."""
    n = min(_samples(seconds), len(samples))
    for k in range(n):
        samples[len(samples) - 1 - k] *= k / n


def _normalise(samples: list[float]) -> list[float]:
    peak = max((abs(s) for s in samples), default=0.0)
    if peak == 0.0:
        return samples
    gain = PEAK / peak
    return [s * gain for s in samples]


def _write_wav(path: Path, samples: list[float]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = bytearray()
    for s in _normalise(samples):
        value = int(round(max(-1.0, min(1.0, s)) * 32767))
        pcm += struct.pack("<h", value)
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(bytes(pcm))


def make_mech(rng: random.Random) -> list[float]:
    """Sharp mechanical click: two noise bursts (8 ms, 6 ms) about 40 ms apart, plus a 1.5 kHz tone."""
    out = [0.0] * _samples(0.15)
    _burst(rng, 0.0, 0.008, out, amp=1.0)
    _burst(rng, 0.040, 0.006, out, amp=0.8)
    _tone(out, freq=1500.0, decay=0.018, amp=0.35, duration=0.15)
    _fade_out(out, 0.004)
    return out


def make_soft(rng: random.Random) -> list[float]:
    """Softer, longer shutter: low-passed noise with a decaying 600 Hz tone, about 0.25 s."""
    n = _samples(0.25)
    noise = [rng.random() * 2.0 - 1.0 for _ in range(n)]
    shaped = _lowpass(noise, alpha=0.12)
    # Shape the whole noise bed with a slow attack and a decay over the duration.
    for k in range(n):
        t = k / SAMPLE_RATE
        attack = min(1.0, t / 0.002)
        shaped[k] *= attack * math.exp(-t / 0.06)
    out = shaped
    _tone(out, freq=600.0, decay=0.05, amp=0.4, duration=0.25)
    _fade_out(out, 0.01)
    return out


def make_disposable(rng: random.Random) -> list[float]:
    """Loud, short click for a disposable camera: about 0.1 s, a 2.4 kHz tone and a short noise tail."""
    out = [0.0] * _samples(0.10)
    _burst(rng, 0.0, 0.005, out, amp=1.0)
    _tone(out, freq=2400.0, decay=0.010, amp=0.5, duration=0.10)
    _burst(rng, 0.006, 0.020, out, amp=0.25)
    _fade_out(out, 0.004)
    return out


def make_gear_tick(rng: random.Random) -> list[float]:
    """Detent click of a mechanical gear under a slider: about 25 ms, a sharp noise tick, a short
    metallic ring at 3.2 kHz and a dull 220 Hz thunk underneath."""
    out = [0.0] * _samples(0.025)
    _burst(rng, 0.0, 0.003, out, amp=0.9)
    _tone(out, freq=3200.0, decay=0.004, amp=0.3, duration=0.025)
    _tone(out, freq=220.0, decay=0.008, amp=0.35, duration=0.025)
    _fade_out(out, 0.003)
    return out


def make_wheel_click(rng: random.Random) -> list[float]:
    """Ratchet click of a camera control wheel: about 60 ms. A pawl strike, a small ring, then a softer
    second tick as the pawl drops into the next tooth, over a dull 160 Hz body thunk."""
    out = [0.0] * _samples(0.06)
    _burst(rng, 0.0, 0.003, out, amp=1.0)
    _tone(out, freq=1900.0, decay=0.006, amp=0.35, duration=0.06)
    _tone(out, freq=160.0, decay=0.014, amp=0.5, duration=0.06)
    _burst(rng, 0.011, 0.002, out, amp=0.55)
    _tone(out, freq=2700.0, decay=0.004, amp=0.2, duration=0.06)
    _fade_out(out, 0.006)
    return out


def make_switch_clunk(rng: random.Random) -> list[float]:
    """Heavier clunk of a mode switch snapping over: about 100 ms, a low thump, a mid knock and a
    second tick when the lever reaches the end of its travel."""
    out = [0.0] * _samples(0.1)
    _burst(rng, 0.0, 0.004, out, amp=0.8)
    _tone(out, freq=110.0, decay=0.03, amp=0.8, duration=0.1)
    _tone(out, freq=720.0, decay=0.01, amp=0.3, duration=0.1)
    _burst(rng, 0.034, 0.003, out, amp=0.6)
    _tone(out, freq=1500.0, decay=0.008, amp=0.25, duration=0.1)
    _fade_out(out, 0.008)
    return _lowpass(out, 0.7)


def main() -> None:
    rng = random.Random(SEED)
    # New sounds go at the end so the seeded noise of the shutter sounds above does not change.
    targets = [
        ("shutter_mech.wav", make_mech),
        ("shutter_soft.wav", make_soft),
        ("shutter_disposable.wav", make_disposable),
        ("slider_tick.wav", make_gear_tick),
        ("wheel_click.wav", make_wheel_click),
        ("switch_clunk.wav", make_switch_clunk),
    ]
    for name, make in targets:
        path = OUT_DIR / name
        _write_wav(path, make(rng))
        print(f"{path} {path.stat().st_size} bytes")


if __name__ == "__main__":
    main()
