package com.kourosh.ae

import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi

/**
 * GPU renderer for the dial core, Kourosh-AE 3.0.
 *
 * The core is shaded as a real sphere instead of a flat disc with a gradient:
 * a surface normal is rebuilt per pixel, then lit with a diffuse term, a tight
 * specular, and a fresnel rim in the state accent. A slow standing wave of
 * "energy" flows under the glass; its strength follows the connection state,
 * so an idle orb is almost still and a connected orb is alive.
 *
 * Every time term is a whole multiple of [phase], which runs 0..2pi with the
 * dial's loop animator, so the motion loops without a visible seam.
 *
 * API 33+ only (RuntimeShader). [createOrNull] swallows compile failures so a
 * driver that rejects the program falls back to the Canvas renderer instead of
 * crashing the main screen.
 */
@RequiresApi(33)
internal class OrbCoreShader private constructor() {

    val shader: RuntimeShader = RuntimeShader(SOURCE)

    fun update(
        cx: Float,
        cy: Float,
        radius: Float,
        phase: Float,
        tiltX: Float,
        tiltY: Float,
        energy: Float,
        accent: Int,
        base: Int,
    ) {
        shader.setFloatUniform("center", cx, cy)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("phase", phase)
        shader.setFloatUniform("tilt", tiltX, tiltY)
        shader.setFloatUniform("energy", energy)
        shader.setColorUniform("accent", accent)
        shader.setColorUniform("base", base)
    }

    companion object {
        fun createOrNull(): OrbCoreShader? = try {
            OrbCoreShader()
        } catch (t: Throwable) {
            null
        }

        private const val SOURCE = """
uniform float2 center;
uniform float radius;
uniform float phase;
uniform float2 tilt;
uniform float energy;
layout(color) uniform half4 accent;
layout(color) uniform half4 base;

half4 main(float2 p) {
    float2 q = (p - center) / radius;
    float d = length(q);
    if (d >= 1.0) {
        return half4(0.0);
    }
    float z = sqrt(max(1.0 - d * d, 0.0));
    float3 n = float3(q.x, q.y, z);
    float3 l = normalize(float3(-0.42 + tilt.x, -0.58 + tilt.y, 0.72));
    float diffuse = max(dot(n, l), 0.0);
    float3 h = normalize(l + float3(0.0, 0.0, 1.0));
    float spec = pow(max(dot(n, h), 0.0), 60.0);
    float fresnel = pow(1.0 - z, 2.2);
    float2 uv = q * 2.4 / (z + 0.55);
    float w = sin(uv.x * 2.7 + phase) * sin(uv.y * 2.3 - phase)
        + 0.6 * sin((uv.x + uv.y) * 1.9 + 2.0 * phase);
    float plasma = smoothstep(0.35, 1.45, w) * energy;
    float3 b = float3(base.rgb);
    float3 a = float3(accent.rgb);
    float3 col = b * (0.52 + 0.48 * diffuse);
    col += a * (fresnel * (0.25 + 0.75 * energy));
    col += a * (plasma * 0.32 * z);
    col += float3(spec * (0.35 + 0.35 * energy));
    col = clamp(col, 0.0, 1.0);
    float edge = 1.0 - smoothstep(1.0 - 2.0 / radius, 1.0, d);
    return half4(half3(col * edge), half(edge));
}
"""
    }
}
