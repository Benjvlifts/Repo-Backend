/**
 * Pruebas unitarias para authService.js y projectService.js
 * Herramienta: Vitest + axios-mock-adapter
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'

// ─── Mock de localStorage ────────────────────────────────────────────────────

const localStorageMock = {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
}
Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// Importar después de mockear localStorage
import { login, register, getUsers, getEmployees } from '../../src/services/authService'
import {
  getProjects, createProject, updateProject, deleteProject,
  updateProjectStatus, assignEmployee, unassignEmployee,
  getNotes, addNote, reviewNote
} from '../../src/services/projectService'

// ─── Setup de mock HTTP ──────────────────────────────────────────────────────

let mockAxios

beforeEach(() => {
  mockAxios = new MockAdapter(axios)
  vi.clearAllMocks()
})

afterEach(() => {
  mockAxios.restore()
})

// ═══════════════════════════════════════════════════════════════════════════
// AUTH SERVICE
// ═══════════════════════════════════════════════════════════════════════════

describe('authService', () => {

  describe('login()', () => {

    it('✅ Retorna datos de usuario al hacer login exitoso', async () => {
      const mockResponse = {
        token: 'jwt_token', type: 'Bearer', userId: 1,
        name: 'Benjamin', email: 'b@test.cl', role: 'EMPLOYEE'
      }
      mockAxios.onPost('/api/auth/login').reply(200, mockResponse)

      const result = await login({ email: 'b@test.cl', password: 'pass123' })

      expect(result).toEqual(mockResponse)
      expect(result.token).toBe('jwt_token')
    })

    it('❌ Lanza error con mensaje del servidor en 401', async () => {
      mockAxios.onPost('/api/auth/login').reply(401, { message: 'Credenciales inválidas' })

      await expect(login({ email: 'bad@test.cl', password: 'wrong' }))
        .rejects.toMatchObject({ message: 'Credenciales inválidas', status: 401 })
    })

    it('❌ Lanza error de conexión si servidor no responde', async () => {
      mockAxios.onPost('/api/auth/login').networkError()

      await expect(login({ email: 'x@test.cl', password: '123' }))
        .rejects.toMatchObject({ message: 'No se pudo conectar con el servidor', status: 0 })
    })
  })

  describe('register()', () => {

    it('✅ Retorna token al registrar usuario nuevo', async () => {
      const mockResponse = {
        token: 'new_jwt', userId: 2, email: 'nuevo@test.cl', role: 'EMPLOYEE'
      }
      mockAxios.onPost('/api/auth/register').reply(201, mockResponse)

      const result = await register({ name: 'Nuevo', email: 'nuevo@test.cl', password: 'pass' })

      expect(result.token).toBe('new_jwt')
      expect(result.email).toBe('nuevo@test.cl')
    })

    it('❌ Lanza error si email ya existe (409)', async () => {
      mockAxios.onPost('/api/auth/register').reply(400, { message: 'El email ya está registrado' })

      await expect(register({ name: 'Test', email: 'exist@test.cl', password: 'pass' }))
        .rejects.toMatchObject({ message: 'El email ya está registrado' })
    })
  })

  describe('getUsers()', () => {

    it('✅ Retorna lista de todos los usuarios', async () => {
      const users = [
        { id: 1, email: 'a@test.cl', role: 'ADMIN' },
        { id: 2, email: 'b@test.cl', role: 'EMPLOYEE' }
      ]
      mockAxios.onGet('/api/auth/users').reply(200, users)

      const result = await getUsers()

      expect(result).toHaveLength(2)
      expect(result[0].role).toBe('ADMIN')
    })

    it('❌ Lanza error si no está autenticado (401)', async () => {
      mockAxios.onGet('/api/auth/users').reply(401, { message: 'No autorizado' })

      await expect(getUsers()).rejects.toMatchObject({ status: 401 })
    })
  })

  describe('getEmployees()', () => {

    it('✅ Filtra solo usuarios con rol EMPLOYEE', async () => {
      const users = [
        { id: 1, email: 'admin@test.cl', role: 'ADMIN' },
        { id: 2, email: 'emp@test.cl', role: 'EMPLOYEE' },
        { id: 3, email: 'mgr@test.cl', role: 'MANAGER' },
      ]
      mockAxios.onGet('/api/auth/users').reply(200, users)

      const result = await getEmployees()

      expect(result).toHaveLength(1)
      expect(result[0].role).toBe('EMPLOYEE')
      expect(result[0].email).toBe('emp@test.cl')
    })

    it('✅ Retorna lista vacía si no hay empleados', async () => {
      mockAxios.onGet('/api/auth/users').reply(200, [
        { id: 1, email: 'admin@test.cl', role: 'ADMIN' }
      ])

      const result = await getEmployees()

      expect(result).toHaveLength(0)
    })
  })
})

// ═══════════════════════════════════════════════════════════════════════════
// PROJECT SERVICE
// ═══════════════════════════════════════════════════════════════════════════

describe('projectService', () => {

  const sampleProject = {
    id: 1, name: 'Portal Cliente', type: 'SOFTWARE',
    status: 'PLANNING', managerId: 10
  }

  describe('getProjects()', () => {

    it('✅ Retorna lista de proyectos', async () => {
      mockAxios.onGet('/api/projects/').reply(200, [sampleProject])

      const result = await getProjects()

      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('Portal Cliente')
    })

    it('✅ Pasa parámetros de filtro al request', async () => {
      mockAxios.onGet('/api/projects/', { params: { status: 'PLANNING' } }).reply(200, [sampleProject])

      const result = await getProjects({ status: 'PLANNING' })

      expect(result).toHaveLength(1)
    })
  })

  describe('createProject()', () => {

    it('✅ Crea proyecto y retorna la respuesta del servidor', async () => {
      const newProject = { name: 'Nuevo Proyecto', type: 'CONSULTING', managerId: 10 }
      mockAxios.onPost('/api/projects/').reply(201, { ...newProject, id: 5, status: 'PLANNING' })

      const result = await createProject(newProject)

      expect(result.id).toBe(5)
      expect(result.name).toBe('Nuevo Proyecto')
    })

    it('❌ Lanza error si validación falla (400)', async () => {
      mockAxios.onPost('/api/projects/').reply(400, { message: 'El nombre es requerido' })

      await expect(createProject({ type: 'SOFTWARE' }))
        .rejects.toMatchObject({ message: 'El nombre es requerido' })
    })
  })

  describe('updateProject()', () => {

    it('✅ Actualiza proyecto y retorna datos actualizados', async () => {
      const updated = { ...sampleProject, name: 'Portal v2' }
      mockAxios.onPut('/api/projects/1').reply(200, updated)

      const result = await updateProject(1, { name: 'Portal v2' })

      expect(result.name).toBe('Portal v2')
    })
  })

  describe('deleteProject()', () => {

    it('✅ Elimina proyecto sin error', async () => {
      mockAxios.onDelete('/api/projects/1').reply(204)

      await expect(deleteProject(1)).resolves.toBeUndefined()
    })

    it('❌ Lanza error si proyecto no existe', async () => {
      mockAxios.onDelete('/api/projects/99').reply(404, { message: 'Proyecto no encontrado' })

      await expect(deleteProject(99)).rejects.toMatchObject({ status: 404 })
    })
  })

  describe('updateProjectStatus()', () => {

    it('✅ Actualiza estado del proyecto con PATCH', async () => {
      mockAxios.onPatch('/api/projects/1/status').reply(200, { ...sampleProject, status: 'IN_PROGRESS' })

      const result = await updateProjectStatus(1, 'IN_PROGRESS')

      expect(result.status).toBe('IN_PROGRESS')
    })
  })

  describe('assignEmployee() / unassignEmployee()', () => {

    it('✅ Asigna empleado al proyecto', async () => {
      mockAxios.onPatch('/api/projects/1/assign').reply(200, { ...sampleProject, assignedUserId: 5 })

      const result = await assignEmployee(1, 5, 'Juan')

      expect(result.assignedUserId).toBe(5)
    })

    it('✅ Desasigna empleado del proyecto', async () => {
      mockAxios.onDelete('/api/projects/1/assign').reply(200, { ...sampleProject, assignedUserId: null })

      const result = await unassignEmployee(1)

      expect(result.assignedUserId).toBeNull()
    })
  })

  describe('Notas de avance', () => {

    const sampleNote = {
      id: 1, projectId: 1, content: 'Avance del módulo',
      status: 'PENDING', authorId: 5
    }

    it('✅ getNotes retorna lista de notas del proyecto', async () => {
      mockAxios.onGet('/api/projects/1/notes').reply(200, [sampleNote])

      const result = await getNotes(1)

      expect(result).toHaveLength(1)
      expect(result[0].status).toBe('PENDING')
    })

    it('✅ addNote crea nota nueva', async () => {
      mockAxios.onPost('/api/projects/1/notes').reply(201, sampleNote)

      const result = await addNote(1, 'Avance del módulo')

      expect(result.content).toBe('Avance del módulo')
    })

    it('✅ reviewNote actualiza estado de la nota', async () => {
      const reviewed = { ...sampleNote, status: 'APPROVED', reviewComment: 'Bien' }
      mockAxios.onPatch('/api/projects/1/notes/1/review').reply(200, reviewed)

      const result = await reviewNote(1, 1, { status: 'APPROVED', reviewComment: 'Bien' })

      expect(result.status).toBe('APPROVED')
    })
  })
})
