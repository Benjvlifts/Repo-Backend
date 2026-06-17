const axios = require('axios');
const { CircuitBreaker } = require('./CircuitBreaker');

const MS_AUTH_URL      = process.env.MS_AUTH_URL      || 'http://localhost:8081/api/auth';
const MS_PROYECTOS_URL = process.env.MS_PROYECTOS_URL || 'http://localhost:8082/api/projects';
const MS_RECURSOS_URL  = process.env.MS_RECURSOS_URL  || 'http://localhost:8083/api/resources';
const CB_THRESHOLD = parseInt(process.env.CIRCUIT_BREAKER_THRESHOLD || '5', 10);
const CB_TIMEOUT   = parseInt(process.env.CIRCUIT_BREAKER_TIMEOUT   || '30000', 10);

const authBreaker      = new CircuitBreaker({ name: 'ms-auth',      failureThreshold: CB_THRESHOLD, timeout: CB_TIMEOUT });
const proyectosBreaker = new CircuitBreaker({ name: 'ms-proyectos', failureThreshold: CB_THRESHOLD, timeout: CB_TIMEOUT });
const recursosBreaker  = new CircuitBreaker({ name: 'ms-recursos',  failureThreshold: CB_THRESHOLD, timeout: CB_TIMEOUT });

const authClient      = axios.create({ baseURL: MS_AUTH_URL,      timeout: 5000 });
const proyectosClient = axios.create({ baseURL: MS_PROYECTOS_URL, timeout: 5000 });
const recursosClient  = axios.create({ baseURL: MS_RECURSOS_URL,  timeout: 5000 });

const httpClient = {
  auth: {
    register:    (data)          => authBreaker.execute(() => authClient.post('/register', data).then(r => r.data)),
    login:       (data)          => authBreaker.execute(() => authClient.post('/login', data).then(r => r.data)),
    validate:    (token)         => authBreaker.execute(() => authClient.post('/validate', { token }).then(r => r.data), () => ({ valid: false })),
    getUsers:    (headers)       => authBreaker.execute(() => authClient.get('/users', { headers }).then(r => r.data)),
    getUserById: (id, headers)   => authBreaker.execute(() => authClient.get(`/users/${id}`, { headers }).then(r => r.data)),
    // Obtener empleados (rol EMPLOYEE) para el selector de asignación
    getEmployees: (headers)      => authBreaker.execute(() => authClient.get('/users', { headers }).then(r =>
      r.data.filter(u => u.role === 'EMPLOYEE')
    )),
  },

  projects: {
    // CRUD básico
    create:       (data, headers)         => proyectosBreaker.execute(() => proyectosClient.post('', data, { headers }).then(r => r.data)),
    getAll:       (params, headers)       => proyectosBreaker.execute(() => proyectosClient.get('', { params, headers }).then(r => r.data), () => []),
    getById:      (id, headers)           => proyectosBreaker.execute(() => proyectosClient.get(`/${id}`, { headers }).then(r => r.data)),

    // Edición completa (Admin)
    update:       (id, data, headers)     => proyectosBreaker.execute(() => proyectosClient.put(`/${id}`, data, { headers }).then(r => r.data)),

    // Estado
    updateStatus: (id, data, headers)     => proyectosBreaker.execute(() => proyectosClient.patch(`/${id}/status`, data, { headers }).then(r => r.data)),

    // Eliminar (Admin)
    delete:       (id, headers)           => proyectosBreaker.execute(() => proyectosClient.delete(`/${id}`, { headers }).then(r => r.data)),

    // Asignación de empleado (Admin + Manager)
    assignEmployee:   (id, data, headers) => proyectosBreaker.execute(() => proyectosClient.patch(`/${id}/assign`, data, { headers }).then(r => r.data)),
    unassignEmployee: (id, headers)       => proyectosBreaker.execute(() => proyectosClient.delete(`/${id}/assign`, { headers }).then(r => r.data)),

    // Notas de avance (tipo PR de GitHub)
    getNotes:   (id, headers)              => proyectosBreaker.execute(() => proyectosClient.get(`/${id}/notes`, { headers }).then(r => r.data), () => []),
    addNote:    (id, data, headers)        => proyectosBreaker.execute(() => proyectosClient.post(`/${id}/notes`, data, { headers }).then(r => r.data)),
    reviewNote: (id, noteId, data, headers) => proyectosBreaker.execute(() => proyectosClient.patch(`/${id}/notes/${noteId}/review`, data, { headers }).then(r => r.data)),
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

  getBreakersStatus: () => ({
    auth:      authBreaker.getStatus(),
    proyectos: proyectosBreaker.getStatus(),
    recursos:  recursosBreaker.getStatus(),
  }),
};

module.exports = { httpClient, authBreaker, proyectosBreaker, recursosBreaker };