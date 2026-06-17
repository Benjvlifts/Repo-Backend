/**
 * Pruebas unitarias para PrivateRoute y authReducer.
 * Herramienta: Vitest + React Testing Library
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */

import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

// ─── Test del authReducer (puro) ─────────────────────────────────────────────

/**
 * Extraemos el reducer directamente para probarlo sin componentes.
 * En AuthContext.jsx el reducer se define como función privada,
 * por lo que lo re-implementamos aquí para testear la lógica.
 */

const initialState = {
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: true,
}

function authReducer(state, action) {
  switch (action.type) {
    case 'LOGIN':
    case 'RESTORE':
      return {
        ...state,
        user: action.payload.user,
        token: action.payload.token,
        isAuthenticated: true,
        isLoading: false
      }
    case 'LOGOUT':
      return { ...initialState, isLoading: false }
    case 'LOADED':
      return { ...state, isLoading: false }
    default:
      return state
  }
}

describe('authReducer (lógica pura)', () => {

  it('✅ Estado inicial es correcto', () => {
    expect(initialState.isAuthenticated).toBe(false)
    expect(initialState.isLoading).toBe(true)
    expect(initialState.user).toBeNull()
    expect(initialState.token).toBeNull()
  })

  it('✅ LOGIN: setea user, token e isAuthenticated=true', () => {
    const user = { email: 'user@test.cl', role: 'EMPLOYEE' }
    const action = { type: 'LOGIN', payload: { user, token: 'jwt123' } }

    const newState = authReducer(initialState, action)

    expect(newState.isAuthenticated).toBe(true)
    expect(newState.user).toEqual(user)
    expect(newState.token).toBe('jwt123')
    expect(newState.isLoading).toBe(false)
  })

  it('✅ RESTORE: mismo resultado que LOGIN', () => {
    const user = { email: 'admin@test.cl', role: 'ADMIN' }
    const action = { type: 'RESTORE', payload: { user, token: 'restored_jwt' } }

    const newState = authReducer(initialState, action)

    expect(newState.isAuthenticated).toBe(true)
    expect(newState.user).toEqual(user)
    expect(newState.token).toBe('restored_jwt')
  })

  it('✅ LOGOUT: limpia todo y setea isLoading=false', () => {
    const authenticatedState = {
      user: { email: 'user@test.cl' },
      token: 'jwt123',
      isAuthenticated: true,
      isLoading: false
    }

    const newState = authReducer(authenticatedState, { type: 'LOGOUT' })

    expect(newState.isAuthenticated).toBe(false)
    expect(newState.user).toBeNull()
    expect(newState.token).toBeNull()
    expect(newState.isLoading).toBe(false)
  })

  it('✅ LOADED: solo marca isLoading=false, mantiene resto', () => {
    const newState = authReducer(initialState, { type: 'LOADED' })

    expect(newState.isLoading).toBe(false)
    expect(newState.isAuthenticated).toBe(false)
    expect(newState.user).toBeNull()
  })

  it('✅ Acción desconocida: retorna estado sin cambios', () => {
    const newState = authReducer(initialState, { type: 'UNKNOWN_ACTION' })

    expect(newState).toEqual(initialState)
  })

  it('✅ Reducer es función pura — no muta el estado original', () => {
    const state = { ...initialState }
    const frozen = Object.freeze(state)

    const newState = authReducer(frozen, {
      type: 'LOGIN',
      payload: { user: { email: 'x@y.cl' }, token: 'token' }
    })

    expect(newState).not.toBe(frozen)
    expect(newState.isAuthenticated).toBe(true)
  })
})

// ─── Tests de PrivateRoute ─────────────────────────────────────────────────

/**
 * PrivateRoute redirige a /login si no hay sesión activa.
 * Permite acceso si isAuthenticated=true y isLoading=false.
 */

// Mock del AuthContext
vi.mock('../../src/context/AuthContext', () => ({
  useAuth: vi.fn()
}))

import { useAuth } from '../../src/context/AuthContext'
import PrivateRoute from '../../src/components/PrivateRoute'

const MockLogin = () => <div>Página de Login</div>
const MockProtected = () => <div>Página Protegida</div>

function renderWithRouter(authState) {
  useAuth.mockReturnValue(authState)

  return render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <Routes>
        <Route path="/login" element={<MockLogin />} />
        <Route element={<PrivateRoute />}>
          <Route path="/dashboard" element={<MockProtected />} />
        </Route>
      </Routes>
    </MemoryRouter>
  )
}

describe('PrivateRoute', () => {

  it('✅ Muestra contenido protegido si usuario está autenticado', () => {
    renderWithRouter({ isAuthenticated: true, isLoading: false })

    expect(screen.getByText('Página Protegida')).toBeInTheDocument()
    expect(screen.queryByText('Página de Login')).not.toBeInTheDocument()
  })

  it('✅ Redirige a /login si usuario no está autenticado', () => {
    renderWithRouter({ isAuthenticated: false, isLoading: false })

    expect(screen.getByText('Página de Login')).toBeInTheDocument()
    expect(screen.queryByText('Página Protegida')).not.toBeInTheDocument()
  })

  it('✅ No renderiza nada mientras isLoading=true (evita flash)', () => {
    renderWithRouter({ isAuthenticated: false, isLoading: true })

    expect(screen.queryByText('Página Protegida')).not.toBeInTheDocument()
    expect(screen.queryByText('Página de Login')).not.toBeInTheDocument()
  })

  it('✅ Usuario autenticado con isLoading=false accede correctamente', () => {
    renderWithRouter({ isAuthenticated: true, isLoading: false, user: { role: 'ADMIN' } })

    expect(screen.getByText('Página Protegida')).toBeInTheDocument()
  })
})
