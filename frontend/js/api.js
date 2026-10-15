/**
 * api.js — Fetch API wrapper for communicating with the Java backend
 * Faculty Leave Management System
 *
 * All API calls go through this module so we can centrally handle:
 *  - Base URL configuration
 *  - Session cookies
 *  - Error handling
 *  - Loading states
 */

const API_BASE = window.location.origin;

const API = {
    /**
     * Generic fetch wrapper
     * @param {string} endpoint - e.g. '/api/leave/apply'
     * @param {object} options  - fetch options
     * @returns {Promise<any>}
     */
    async request(endpoint, options = {}) {
        const url = `${API_BASE}${endpoint}`;

        const defaults = {
            credentials: 'include',  // send session cookie
            headers: {
                'Accept': 'application/json',
            },
        };

        // Merge headers
        if (options.body && !(options.body instanceof FormData)) {
            defaults.headers['Content-Type'] = 'application/json';
        }

        const config = {
            ...defaults,
            ...options,
            headers: { ...defaults.headers, ...options.headers },
        };

        try {
            const response = await fetch(url, config);

            // Handle 401 — redirect to login
            if (response.status === 401) {
                if (!window.location.pathname.endsWith('index.html') &&
                    window.location.pathname !== '/') {
                    window.location.href = 'index.html';
                }
                throw new Error('Session expired. Please sign in again.');
            }

            // Handle 403 — forbidden
            if (response.status === 403) {
                throw new Error('You do not have permission to perform this action.');
            }

            // Try to parse JSON
            const contentType = response.headers.get('Content-Type') || '';
            let data;

            if (contentType.includes('application/json')) {
                data = await response.json();
            } else if (contentType.includes('text/csv')) {
                // For CSV export responses
                return await response.blob();
            } else {
                data = await response.text();
            }

            // Handle non-OK responses
            if (!response.ok) {
                const errorMsg = data?.error || data?.message || `Request failed (${response.status})`;
                throw new Error(errorMsg);
            }

            return data;
        } catch (error) {
            if (error.name === 'TypeError' && error.message.includes('fetch')) {
                throw new Error('Unable to connect to the server. Please check your connection.');
            }
            throw error;
        }
    },

    /**
     * GET request
     * @param {string} endpoint
     * @param {object} params - query parameters
     * @returns {Promise<any>}
     */
    async get(endpoint, params = {}) {
        // Build query string
        const queryParts = [];
        for (const [key, value] of Object.entries(params)) {
            if (value !== undefined && value !== null && value !== '') {
                queryParts.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`);
            }
        }
        const query = queryParts.length ? `?${queryParts.join('&')}` : '';

        return this.request(`${endpoint}${query}`, { method: 'GET' });
    },

    /**
     * POST request with JSON body
     * @param {string} endpoint
     * @param {object} body
     * @returns {Promise<any>}
     */
    async post(endpoint, body = {}) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(body),
        });
    },

    /**
     * PUT request with JSON body
     * @param {string} endpoint
     * @param {object} body
     * @returns {Promise<any>}
     */
    async put(endpoint, body = {}) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(body),
        });
    },

    /**
     * DELETE request
     * @param {string} endpoint
     * @returns {Promise<any>}
     */
    async delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    },

    /**
     * POST with FormData (for file uploads)
     * @param {string} endpoint
     * @param {FormData} formData
     * @returns {Promise<any>}
     */
    async upload(endpoint, formData) {
        return this.request(endpoint, {
            method: 'POST',
            body: formData,
            // Don't set Content-Type — browser sets it with boundary
        });
    },
};

/**
 * Fetch the application config (Google Client ID, etc.)
 * Called once on page load
 */
async function loadAppConfig() {
    try {
        const config = await API.get('/api/config');
        window.APP_CONFIG = config;
    } catch (e) {
        console.warn('Could not load app config:', e.message);
        window.APP_CONFIG = {};
    }
}

// Load config on script load
loadAppConfig();
