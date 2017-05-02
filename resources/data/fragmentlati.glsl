void main(void) {
    if (cutOffRadius.x > 1.)
        discard;

    vec2 normalizedScreenpos = (gl_FragCoord.xy - viewportOffset) / viewport - .5;
    normalizedScreenpos *= 2. * vec2(viewport.y / viewport.x, 1.);
    vec4 scrpos = cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.) + .5;
    clamp_texcoord(scrpos.xy);

    float theta = scrpos.y * PI;
    float phi = PI + hgln + scrpos.x * TWOPI;
    while (phi > TWOPI)
        phi -= TWOPI;
    /*
    if( phi > PI/32. && phi<TWOPI - PI/32.){
        discard;
    }
    */

    vec3 xcart;
    xcart.x = sin(theta) * cos(phi);
    xcart.y = sin(theta) * sin(phi);
    xcart.z = cos(theta);
    mat3 rot = mat3(
          cos(hglt), 0., sin(hglt),
          0.,        1., 0.,
         -sin(hglt), 0., cos(hglt)
    );
    mat3 crot = mat3(
         1.,  0.,         0.,
         0.,  cos(crota), sin(crota),
         0., -sin(crota), cos(crota)
    );
    vec3 xcartrot = crot * rot * xcart;
    if (xcartrot.x < 0.)
        discard;

    vec2 texcoord = vec2(rect.w * (xcartrot.y - rect.x), rect.w * (xcartrot.z - rect.y));
    clamp_texcoord(texcoord);
    gl_FragColor = getColor(texcoord, texcoord, 1.);
}
