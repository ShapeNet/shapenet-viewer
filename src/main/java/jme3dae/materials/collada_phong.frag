uniform bool useAmbientColor;
uniform bool useDiffuseColor;
uniform bool useEmissiveColor;
uniform bool useAmbientTexture;
uniform bool useDiffuseTexture;
uniform bool useEmissiveTexture;
uniform vec4 ambientColor;
uniform vec4 diffuseColor;
uniform vec4 emissiveColor;
uniform sampler2D ambientTexture;
uniform sampler2D diffuseTexture;
uniform sampler2D emissiveTexture;

varying vec2 texCoord;

void main() {

	vec4 diffuse;
	vec4 emissive;
	vec4 ambient;

	if(useAmbientColor) {
		ambient = ambientColor;
	}
	if(useDiffuseColor) {
		diffuse = diffuseColor;
	}
	if(useEmissiveColor) {
		emissive = emissiveColor;
	}
	if(useAmbientTexture) {
		vec4 texColor = texture2D(ambientTexture, texCoord);
		if(useAmbientColor) {
			ambient = vec4(mix(ambient.rgb, texColor.rgb, texColor.a), 1);
		} else {
			ambient = texColor;
		}
	}
	if(useDiffuseTexture) {
		vec4 texColor = texture2D(diffuseTexture, texCoord);
		if(useDiffuseColor) {
			diffuse = vec4(mix(diffuse.rgb, texColor.rgb, texColor.a), 1);
		} else {
			diffuse = texColor;
		}
	}
	if(useEmissiveTexture) {
		vec4 texColor = texture2D(emissiveTexture, texCoord);
		if(useEmissiveColor) {
			diffuse = vec4(mix(emissive.rgb, texColor.rgb, texColor.a), 1);
		} else {
			emissive = texColor;
		}
	}
	vec4 finalColor = mix(ambient, diffuse, 0.5);
	finalColor = mix(finalColor, emissive, 0.5);
	gl_FragColor = finalColor;
}