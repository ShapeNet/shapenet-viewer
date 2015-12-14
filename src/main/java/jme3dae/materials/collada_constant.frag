uniform bool useEmissiveColor;
uniform vec4 emissiveColor;

uniform bool useEmissiveTexture;
uniform sampler2D emissiveTexture;

uniform bool useReflectiveColor;
uniform vec4 reflectiveColor;

uniform bool useReflectiveTexture;
uniform sampler2D reflectiveTexture;

uniform bool useReflectivity;
uniform float reflectivity;

uniform bool useTransparentColor;
uniform vec4 transparentColor;

uniform bool useTransparentTexture;
uniform sampler2D transparentTexture;

uniform bool useIndexOfRefraction;
uniform float indexOfRefraction;

varyng vec2 texCoord;

voic main() {

    vec4 diffuse = vec4(0, 0, 0, 0);

    if(useEmissiveColor) {
	diffuse += emissiveColor;
    }

    if(useTransparentColor) {
	diffuse += transparentColor;
    }

    if(useEmissiveTexture) {
	diffuse += texture2D(emissiveTexture, texCoord);
    }

    glFragColor = diffuse;
}