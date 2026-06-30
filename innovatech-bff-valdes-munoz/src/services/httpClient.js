const axios = require('axios');
const { CircuitBreaker } = require('./CircuitBreaker');

// ROOT CAUSE BUG 1: docker-compose pasa sólo el host:puerto (ej. http://ms-auth:8081).
// El path de la API (/api/auth, /api/projects…) debe concatenarse aquí,
// no asumirse dentro de la variable de entorno.
const strip = (url) => (url || '').replace(/\/+$/, '');

const MS_AUTH_BASE      = strip(process.env.MS_AUTH_URL)      || 'http://localhost:8081';
const MS_PROYECTOS_BASE = strip(process.env.MS_PROYECTOS_URL) || 'http://localhost:8082';
const MS_RECURSOS_BASE  = strip(process.env.MS_RECURSOS_URL)  || 'http://localhost:8083';
const MS_ANALITICA_BASE = strip(process.env.MS_ANALITICA_URL) || 'http://localhost:8084';
const MS_NOTIF_BASE     = strip(process.env.MS_NOTIF_URL)     || 'http://localhost:8085';

const CB_THRESHOLD = parseInt(process.env.CIRCUIT_BREAKER_THRESHOLD || '5', 10);
const CB_TIMEOUT   = parseInt(process.env.CIRCUIT_BREAKER_TIMEOUT   || '30000', 10);

const authBreaker      = new CircuitBreaker({ name: 'ms-auth',      failureThreshold: CB_THRESHOLD, timeout: CB_TIMEOUT });
const proyectosBreaker = new CircuitBreaker({ name: 'ms-proyectos', failureThreshold: CB_THRESHOLD, timeout: CB_TIMEOUT });
const recursosBreaker  = new CircuitBreaker({ name: 'ms-recursos',  failureThreshold: CB_THRESHOLD, timeout: CB_TIMEOUT });
const analiticaBreaker = new CircuitBreaker({ name: 'ms-analitica', failureThreshold: CB_THRESHOLD, timeout: CB_TIMEOUT });
const notifBreaker     = new CircuitBreaker({ name: 'ms-notif',     failureThreshold: CB_THRESHOLD, timeout: CB_TIMEOUT });

// Los paths de API coinciden con @RequestMapping de cada controlador Spring Boot
const authClient      = axios.create({ baseURL: `${MS_AUTH_BASE}/api/auth`,               timeout: 5000 });
const proyectosClient = axios.create({ baseURL: `${MS_PROYECTOS_BASE}/api/projects`,      timeout: 5000 });
const recursosClient  = axios.create({ baseURL: `${MS_RECURSOS_BASE}/api/resources`,      timeout: 5000 });
const analiticaClient = axios.create({ baseURL: `${MS_ANALITICA_BASE}/api/v1/analitica`,  timeout: 5000 });
const notifClient     = axios.create({ baseURL: `${MS_NOTIF_BASE}/api/v1/notificaciones`, timeout: 5000 });

const httpClient = {
  auth: {
    register:    (data)               => authBreaker.execute(() => authClient.post('/register', data).then(r => r.data)),
    login:       (data)               => authBreaker.execute(() => authClient.post('/login', data).then(r => r.data)),
    validate:    (token)              => authBreaker.execute(() => authClient.post('/validate', { token }).then(r => r.data), () => ({ valid: false })),
    getUsers:    (headers)            => authBreaker.execute(() => authClient.get('/users', { headers }).then(r => r.data)),
    getUserById: (id, headers)        => authBreaker.execute(() => authClient.get(`/users/${id}`, { headers }).then(r => r.data)),
    getEmployees:(headers)            => authBreaker.execute(() => authClient.get('/users', { headers }).then(r =>
      r.data.filter(u => u.role === 'EMPLOYEE')
    )),
  },

  projects: {
    create:           (data, headers)             => proyectosBreaker.execute(() => proyectosClient.post('', data, { headers }).then(r => r.data)),
    getAll:           (params, headers)           => proyectosBreaker.execute(() => proyectosClient.get('', { params, headers }).then(r => r.data), () => []),
    getById:          (id, headers)               => proyectosBreaker.execute(() => proyectosClient.get(`/${id}`, { headers }).then(r => r.data)),
    update:           (id, data, headers)         => proyectosBreaker.execute(() => proyectosClient.put(`/${id}`, data, { headers }).then(r => r.data)),
    updateStatus:     (id, data, headers)         => proyectosBreaker.execute(() => proyectosClient.patch(`/${id}/status`, data, { headers }).then(r => r.data)),
    delete:           (id, headers)               => proyectosBreaker.execute(() => proyectosClient.delete(`/${id}`, { headers }).then(r => r.data)),
    assignEmployee:   (id, data, headers)         => proyectosBreaker.execute(() => proyectosClient.patch(`/${id}/assign`, data, { headers }).then(r => r.data)),
    unassignEmployee: (id, headers)               => proyectosBreaker.execute(() => proyectosClient.delete(`/${id}/assign`, { headers }).then(r => r.data)),
    getNotes:         (id, headers)               => proyectosBreaker.execute(() => proyectosClient.get(`/${id}/notes`, { headers }).then(r => r.data), () => []),
    addNote:          (id, data, headers)         => proyectosBreaker.execute(() => proyectosClient.post(`/${id}/notes`, data, { headers }).then(r => r.data)),
    reviewNote:       (id, noteId, data, headers) => proyectosBreaker.execute(() => proyectosClient.patch(`/${id}/notes/${noteId}/review`, data, { headers }).then(r => r.data)),
  },

  resources: {
    create:             (data, headers)     => recursosBreaker.execute(() => recursosClient.post('', data, { headers }).then(r => r.data)),
    getAll:             (headers)           => recursosBreaker.execute(() => recursosClient.get('', { headers }).then(r => r.data), () => []),
    getAvailable:       (headers)           => recursosBreaker.execute(() => recursosClient.get('/available', { headers }).then(r => r.data), () => []),
    getById:            (id, headers)       => recursosBreaker.execute(() => recursosClient.get(`/${id}`, { headers }).then(r => r.data)),
    getByDepartment:    (dept, headers)     => recursosBreaker.execute(() => recursosClient.get(`/department/${dept}`, { headers }).then(r => r.data)),
    updateAvailability: (id, data, headers) => recursosBreaker.execute(() => recursosClient.patch(`/${id}/availability`, data, { headers }).then(r => r.data)),
    assignToProject:    (id, data, headers) => recursosBreaker.execute(() => recursosClient.patch(`/${id}/assign`, data, { headers }).then(r => r.data)),
    delete:             (id, headers)       => recursosBreaker.execute(() => recursosClient.delete(`/${id}`, { headers }).then(r => r.data)),
  },

  analitica: {
    getAllMetrics:      (headers)            => analiticaBreaker.execute(() => analiticaClient.get('/metricas', { headers }).then(r => r.data), () => []),
    getProjectMetrics: (projectId, headers) => analiticaBreaker.execute(() => analiticaClient.get(`/metricas/proyecto/${projectId}`, { headers }).then(r => r.data)),
    getSummary:        (headers)            => analiticaBreaker.execute(() => analiticaClient.get('/resumen', { headers }).then(r => r.data), () => null),
  },

  notificaciones: {
    getByProject: (projectId, headers) => notifBreaker.execute(() => notifClient.get(`/proyecto/${projectId}`, { headers }).then(r => r.data), () => []),
    getUnread:    (projectId, headers) => notifBreaker.execute(() => notifClient.get(`/proyecto/${projectId}/no-leidas`, { headers }).then(r => r.data), () => []),
    markAsRead:   (id, headers)        => notifBreaker.execute(() => notifClient.patch(`/${id}/read`, {}, { headers }).then(r => r.data)),
  },

  getBreakersStatus: () => ({
    auth:      authBreaker.getStatus(),
    proyectos: proyectosBreaker.getStatus(),
    recursos:  recursosBreaker.getStatus(),
    analitica: analiticaBreaker.getStatus(),
    notif:     notifBreaker.getStatus(),
  }),
};

module.exports = { httpClient, authBreaker, proyectosBreaker, recursosBreaker, analiticaBreaker, notifBreaker };