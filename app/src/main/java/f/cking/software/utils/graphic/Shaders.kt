package f.cking.software.utils.graphic

import org.intellij.lang.annotations.Language

object Shaders {
    @Language("AGSL")
    val SHADER_CONTENT = """
        uniform shader content;
    
        uniform float blurredHeight;
        uniform float2 iResolution;
        
        float4 main(float2 coord) {
            if (coord.y > iResolution.y - blurredHeight) { // Blur the bottom part of the screen
                return float4(1.0, 1.0, 1.0, 1.0);
            } else {
                return content.eval(coord);
            }
        }
"""

    @Language("AGSL")
    val SHADER_BLURRED = """
        uniform shader content;
    
        uniform float blurredHeight;
        uniform float2 iResolution;
        
        float4 main(float2 coord) {
            if (coord.y > iResolution.y - blurredHeight) { // Blur the bottom part of the screen
                return content.eval(coord);
            } else {
                return float4(0.0, 0.0, 0.0, 0.0);
            }
        }
    """

    @Language("AGSL")
    val GLASS_SHADER = """
        uniform shader content;
        uniform float blurredHeight;
        uniform float2 iResolution;
        
        uniform float horizontalSquareSize;
        const float verticalSquares = 1.0;
        const float verticalOffset = 0.1;
        const float horizontalOffset = 0.05;
        
        const float amt = 0.1;
        
        float4 gradient(float2 coordOriginal) {
            float2 coord = float2(coordOriginal.x, coordOriginal.y - iResolution.y * 0.5 + blurredHeight);
            float2 pos_ndc = 2.0 * coord.xy / iResolution.xy - 1.0;
            float dist = length(pos_ndc);
        
            vec4 color1 = vec4(0.0, 0.0, 0.0, 1.0);
            vec4 color2 = vec4(0.95, 0.95, 0.95, 1.0);
            vec4 color3 = vec4(0.0, 0.0, 0.0, 1.0);
            vec4 color4 = vec4(0.95, 0.95, 1.0, 0.95);
            float step1 = 0.0;
            float step2 = 0.33;
            float step3 = 0.66;
            float step4 = 1.0;
        
            vec4 color = mix(color1, color2, smoothstep(step1, step2, dist));
            color = mix(color, color3, smoothstep(step2, step3, dist));
            color = mix(color, color4, smoothstep(step3, step4, dist));
        
            return color;
        }
        
        float4 colorDistortion(float2 fragCoord) {
            // uv (0 to 1)
            float2 uv = fragCoord.xy / iResolution.xy;
        
            float chromo_x = 0.025;
            float chromo_y = 0.025;
            
            return float4(content.eval(float2(uv.x - chromo_x * 0.016, uv.y - chromo_y * 0.009) * iResolution.xy).r, content.eval(float2(uv.x + chromo_x * 0.0125, uv.y - chromo_y * 0.004) * iResolution.xy).g, content.eval(float2(uv.x - chromo_x * 0.0045, uv.y + chromo_y * 0.0085) * iResolution.xy).b, 1.0);
        }
        
        float2 sphericalTransformation(
            float u,
            float v,
            float uCenter,
            float vCenter,
            float lensRadius,
            float tau
        ) {
            u -= uCenter;
            v -= vCenter;
            
            float l = sqrt(u * u + v * v);
            float z = sqrt(lensRadius * lensRadius - l * l);
            
            float sphereRadius = sqrt(u * u + v * v + z * z);
        
            float uAlpha = (1.0 - (1.0 / tau)) * sin(u / sphereRadius / 2);
            float vAlpha = (1.0 - (1.0 / tau)) * sin(v / sphereRadius);
            
            u = l <= lensRadius ?
                u + uCenter - z * tan(uAlpha) :
                u + uCenter;
                
            v = l <= lensRadius ?
                v + vCenter - z * tan(vAlpha) :
                v + vCenter;
            
            return float2(u, v);
        }

        float4 main(float2 fragCoord) {
        
            float2 offset = float2(horizontalOffset, verticalOffset);
            float2 squares = float2(iResolution.x / horizontalSquareSize, verticalSquares);
        	float2 uv = fragCoord.xy / iResolution.xy;
            
            float2 tc = uv;
            tc.x *= iResolution.x / iResolution.y;
            
            float2 tile = fract(tc * squares);

            uv = sphericalTransformation(
                uv.x,
                uv.y,
                (uv + (tile * amt) - offset).x,
                1.0,
                (blurredHeight / iResolution.y * 1.5),
                1.5
            );
            
            float2 flutedGlassCoordinate = (uv + (tile * amt) - offset) * iResolution.xy;
            float4 color = colorDistortion(flutedGlassCoordinate);
            float4 white = float4(1.0, 1.0, 1.0, 1.0);
            float4 colorModificator = 0.04 * gradient((uv + (tile * amt) - offset) * iResolution.xy);
        	return min(color + colorModificator, white);
        }
    """


    @Language("AGSL")
    val WATER_DROP = """
        uniform shader content;
        uniform float factor;
        uniform float2 iResolution;
        uniform float2 dropPosition;
        uniform float timeFactor;
        
        const float PI = 3.14159265359;
        
        const float3 eps = float3(0.01, 0.0, 0.0);
        
        float genWave(float len)
        {
        	float wave = sin(1.0 * PI * len - (timeFactor * 5.0 + 2.0));
        	wave = (wave + 1.0) * (0.5 * factor); // <0 ; 1>
        	wave -= (0.3 * factor);
        	wave *= wave * wave;
        	return wave;
        }
        
        float scene(float len)
        {
        	return genWave(len);
        }
        
        float2 normal(float len) 
        {
        	float tg = (scene(len + eps.x) - scene(len)) / eps.x;
        	return normalize(float2(-tg, 1.0));
        }
        
        float4 main(float2 fragCoord)
        {
            if (factor == 0.0) {
                return content.eval(fragCoord);
            }
        	float2 uv = fragCoord.xy / iResolution.xy;
        	float2 so = dropPosition.xy / iResolution.xy;
        	float2 pos2 = float2(uv - so); 	  //wave origin
        	float2 pos2n = normalize(pos2);
        
        	float len = length(pos2);
        	float wave = scene(len); 
        
        	float2 uvR = -pos2n * wave/(1.0 + 5.0 * len + 0.2);
        	float2 uvG = -pos2n * wave/(1.0 + 5.0 * len + 0.1);
        	float2 uvB = -pos2n * wave/(1.0 + 5.0 * len);
            
        	return float4(content.eval((uv + uvR) * iResolution.xy).r, content.eval((uv + uvG) * iResolution.xy).g, content.eval((uv + uvB) * iResolution.xy).b, content.eval(fragCoord).a);
        }
    """
}