/**
 * Helper function to build clean URLs without empty parameters
 * @param {string} baseUrl - The base URL path
 * @param {Object} params - An object containing URL parameters
 * @returns {string} - The constructed URL with only non-empty parameters
 */
function buildCleanUrl(baseUrl, params) {
    const cleanParams = {};
    for (const [key, value] of Object.entries(params)) {
        // Convert to string and trim to handle various empty states
        const stringValue = String(value).trim();
        
        // Skip null, undefined, empty strings, and string representations
        if (value === null || value === undefined || stringValue === '' || 
            stringValue === 'null' || stringValue === 'undefined') {
            continue;
        }
        
        // Skip boolean false or string "false" for showFirmAdmins (default)
        if (key === 'showFirmAdmins' && (value === false || stringValue === 'false')) {
            continue;
        }
        
        // Skip default values
        if (key === 'size' && value == 10) continue;
        if (key === 'page' && value == 1) continue;
        
        cleanParams[key] = value;
    }
    
    const queryString = Object.entries(cleanParams)
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
        .join('&');
    return queryString ? `${baseUrl}?${queryString}` : baseUrl;
}
