/**
 * Helper function to build clean URLs without empty parameters.
 * Supports array values — each element is appended as a separate param.
 * @param {string} baseUrl - The base URL path
 * @param {Object} params - An object containing URL parameters (values may be arrays)
 * @returns {string} - The constructed URL with only non-empty parameters
 */
function buildCleanUrl(baseUrl, params) {
    const parts = [];

    for (const [key, value] of Object.entries(params)) {
        const values = Array.isArray(value) ? value : [value];

        for (const v of values) {
            const stringValue = String(v).trim();

            // Skip null, undefined, empty strings
            if (v === null || v === undefined || stringValue === '' ||
                stringValue === 'null' || stringValue === 'undefined') {
                continue;
            }

            // Skip false booleans for filter flags (default state)
            if ((key === 'showFirmAdmins' || key === 'showMultiFirmUsers' || key === 'showProviderUsers')
                    && (v === false || stringValue === 'false')) {
                continue;
            }

            // Skip default pagination values
            if (key === 'size' && v == 10) continue;
            if (key === 'page' && v == 1) continue;

            parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(stringValue)}`);
        }
    }

    const queryString = parts.join('&');
    return queryString ? `${baseUrl}?${queryString}` : baseUrl;
}
