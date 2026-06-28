/**
 * Pruebas unitarias para AuthContext (React Context + useReducer).
 * Herramienta: Vitest + React Testing Library
 *
 * Ejecutar: npm run test (en directorio innovatech-frontend-valdes-munoz)
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, act, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthProvider, useAuth } from '../../src/context/AuthContext'

// ─── Mock de localStorage ────────────────────────────────────────────────────

const localStorageMock = (() => {
  let store = {}
  return {
    getItem: vi.fn((key) => store[key] ?? null),
    setItem: vi.fn((key, value) => { store[key] = value }),
    removeItem: vi.fn((key) => { delete store[key] }),
    clear: vi.fn(() => { store = {} }),
  }
})()

Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// ─── Componente auxiliar para testear el hook ─────────────────────────────────

function TestConsumer() {
  const { user, token, isAuthenticated, isLoading, login, logout } = useAuth()
  return (
    <div>
      <span data-testid="authenticated">{String(isAuthenticated)}</span>
      <span data-testid="loading">{String(isLoading)}</span>
      <span data-testid="user">{user ? user.email : 'null'}</span>
      <span data-testid="token">{token ?? 'null'}</span>
      <button onClick={() => login({ email: 'test@test.cl', role: 'EMPLOYEE' }, 'jwt123')}>
        Login
      </button>
      <button onClick={logout}>Logout</button>
    </div>
  )
}

// ─── Tests ───────────────────────────────────────────────────────────────────

describe('AuthContext', () => {

  beforeEach(() => {
    localStorageMock.clear()
    vi.clearAllMocks()
  })

  afterEach(() => {
    localStorageMock.clear()
  })

  describe('Estado inicial', () => {

    it('✅ isLoading comienza en true y se resuelve a false', async () => {
      localStorageMock.getItem.mockReturnValue(null)

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('loading').textContent).toBe('false')
      })
    })

    it('✅ Sin sesión previa: isAuthenticated = false, user = null', async () => {
      localStorageMock.getItem.mockReturnValue(null)

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('authenticated').textContent).toBe('false')
        expect(screen.getByTestId('user').textContent).toBe('null')
        expect(screen.getByTestId('token').textContent).toBe('null')
      })
    })

    it('✅ Restaura sesión desde localStorage si existe token y usuario', async () => {
      const storedUser = { email: 'stored@test.cl', role: 'ADMIN' }
      localStorageMock.getItem
        .mockImplementation((key) => {
          if (key === 'innovatech_token') return 'stored_jwt_token'
          if (key === 'innovatech_user') return JSON.stringify(storedUser)
          return null
        })

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('authenticated').textContent).toBe('true')
        expect(screen.getByTestId('user').textContent).toBe('stored@test.cl')
        expect(screen.getByTestId('token').textContent).toBe('stored_jwt_token')
      })
    })

    it('✅ Maneja JSON inválido en localStorage sin crash', async () => {
      localStorageMock.getItem
        .mockImplementation((key) => {
          if (key === 'innovatech_token') return 'some_token'
          if (key === 'innovatech_user') return 'INVALID_JSON{{{'
          return null
        })

      // No debe lanzar error
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('loading').textContent).toBe('false')
        expect(screen.getByTestId('authenticated').textContent).toBe('false')
      })
    })
  })

  describe('login()', () => {

    it('✅ Actualiza estado a autenticado con user y token', async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      )

      await waitFor(() => expect(screen.getByTestId('loading').textContent).toBe('false'))

      await act(async () => {
        await userEvent.click(screen.getByText('Login'))
      })

      expect(screen.getByTestId('authenticated').textContent).toBe('true')
      expect(screen.getByTestId('user').textContent).toBe('test@test.cl')
      expect(screen.getByTestId('token').textContent).toBe('jwt123')
    })

    it('✅ Persiste token en localStorage al hacer login', async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      )

      await waitFor(() => expect(screen.getByTestId('loading').textContent).toBe('false'))
      await act(async () => { await userEvent.click(screen.getByText('Login')) })

      expect(localStorageMock.setItem).toHaveBeenCalledWith('innovatech_token', 'jwt123')
      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'innovatech_user',
        JSON.stringify({ email: 'test@test.cl', role: 'EMPLOYEE' })
      )
    })
  })

  describe('logout()', () => {

    it('✅ Limpia estado y localStorage al hacer logout', async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      )

      await waitFor(() => expect(screen.getByTestId('loading').textContent).toBe('false'))

      // Login primero
      await act(async () => { await userEvent.click(screen.getByText('Login')) })
      expect(screen.getByTestId('authenticated').textContent).toBe('true')

      // Luego logout
      await act(async () => { await userEvent.click(screen.getByText('Logout')) })

      expect(screen.getByTestId('authenticated').textContent).toBe('false')
      expect(screen.getByTestId('user').textContent).toBe('null')
      expect(screen.getByTestId('token').textContent).toBe('null')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('innovatech_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('innovatech_user')
    })
  })

  describe('useAuth() fuera de AuthProvider', () => {

    it('❌ Lanza error si useAuth se usa fuera de AuthProvider', () => {
      // Suprimir error de consola esperado
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

      expect(() => render(<TestConsumer />)).toThrow('useAuth debe usarse dentro de AuthProvider')

      consoleSpy.mockRestore()
    })
  })
})