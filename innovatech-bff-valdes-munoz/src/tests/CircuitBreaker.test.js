const { CircuitBreaker, STATES } = require('../services/CircuitBreaker');

describe('CircuitBreaker', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  // ── Estado CLOSED ──────────────────────────────────────────────────────────
  describe('Estado CLOSED', () => {
    test('inicia en estado CLOSED con contadores en cero', () => {
      const cb = new CircuitBreaker({ name: 'test-service' });

      expect(cb.state).toBe(STATES.CLOSED);
      expect(cb.failureCount).toBe(0);
      expect(cb.successCount).toBe(0);
    });

    test('ejecuta la función y retorna su resultado cuando tiene éxito', async () => {
      const cb = new CircuitBreaker({ name: 'test-service' });
      const fn = jest.fn().mockResolvedValue('ok');

      const result = await cb.execute(fn);

      expect(result).toBe('ok');
      expect(fn).toHaveBeenCalledTimes(1);
      expect(cb.state).toBe(STATES.CLOSED);
      expect(cb.successCount).toBe(1);
    });

    test('permanece en CLOSED tras fallos por debajo del umbral', async () => {
      const cb = new CircuitBreaker({ name: 'test-service', failureThreshold: 5 });
      const fn = jest.fn().mockRejectedValue(new Error('fallo simulado'));

      for (let i = 0; i < 4; i++) {
        await expect(cb.execute(fn)).rejects.toThrow('fallo simulado');
      }

      expect(cb.state).toBe(STATES.CLOSED);
      expect(cb.failureCount).toBe(4);
    });
  });

  // ── Transición CLOSED → OPEN ─────────────────────────────────────────────────
  describe('Transición a OPEN', () => {
    test('abre el circuito al alcanzar el failureThreshold', async () => {
      const cb = new CircuitBreaker({ name: 'test-service', failureThreshold: 3 });
      const fn = jest.fn().mockRejectedValue(new Error('fallo simulado'));

      await expect(cb.execute(fn)).rejects.toThrow('fallo simulado');
      await expect(cb.execute(fn)).rejects.toThrow('fallo simulado');
      await expect(cb.execute(fn)).rejects.toThrow('fallo simulado');

      expect(cb.state).toBe(STATES.OPEN);
      expect(cb.failureCount).toBe(3);
    });

    test('usa el fallback en vez de propagar el error cuando se provee', async () => {
      const cb = new CircuitBreaker({ name: 'test-service', failureThreshold: 1 });
      const fn = jest.fn().mockRejectedValue(new Error('fallo simulado'));
      const fallback = jest.fn().mockReturnValue('valor-por-defecto');

      const result = await cb.execute(fn, fallback);

      expect(result).toBe('valor-por-defecto');
      expect(fallback).toHaveBeenCalledTimes(1);
      expect(cb.state).toBe(STATES.OPEN);
    });
  });

  // ── Estado OPEN ────────────────────────────────────────────────────────────
  describe('Estado OPEN', () => {
    test('rechaza inmediatamente con error 503 sin invocar la función mientras el timeout no expira', async () => {
      const cb = new CircuitBreaker({ name: 'test-service', failureThreshold: 1, timeout: 30000 });
      const failingFn = jest.fn().mockRejectedValue(new Error('fallo simulado'));

      await expect(cb.execute(failingFn)).rejects.toThrow('fallo simulado');
      expect(cb.state).toBe(STATES.OPEN);

      const fn = jest.fn().mockResolvedValue('no debería llamarse');
      await expect(cb.execute(fn)).rejects.toMatchObject({ statusCode: 503 });
      expect(fn).not.toHaveBeenCalled();
    });

    test('usa el fallback cuando el circuito está OPEN y no ha expirado el timeout', async () => {
      const cb = new CircuitBreaker({ name: 'test-service', failureThreshold: 1, timeout: 30000 });
      const failingFn = jest.fn().mockRejectedValue(new Error('fallo simulado'));
      await expect(cb.execute(failingFn)).rejects.toThrow('fallo simulado');
      expect(cb.state).toBe(STATES.OPEN);

      const fallback = jest.fn().mockReturnValue('fallback-open');
      const fn = jest.fn();

      const result = await cb.execute(fn, fallback);

      expect(result).toBe('fallback-open');
      expect(fn).not.toHaveBeenCalled();
    });
  });

  // ── Transición OPEN → HALF_OPEN ──────────────────────────────────────────────
  describe('Transición a HALF_OPEN', () => {
    test('pasa a HALF_OPEN y ejecuta la función de prueba una vez expirado el timeout', async () => {
      const cb = new CircuitBreaker({ name: 'test-service', failureThreshold: 1, timeout: 30000 });
      const failingFn = jest.fn().mockRejectedValue(new Error('fallo simulado'));
      await expect(cb.execute(failingFn)).rejects.toThrow('fallo simulado');
      expect(cb.state).toBe(STATES.OPEN);

      const realNow = Date.now;
      jest.spyOn(Date, 'now').mockImplementation(() => realNow() + 30001);

      const probeFn = jest.fn().mockResolvedValue('recuperado');
      const result = await cb.execute(probeFn);

      expect(probeFn).toHaveBeenCalledTimes(1);
      expect(result).toBe('recuperado');
    });

    test('cierra el circuito (CLOSED) si la petición de prueba en HALF_OPEN tiene éxito', async () => {
      const cb = new CircuitBreaker({ name: 'test-service', failureThreshold: 1, timeout: 30000 });
      const failingFn = jest.fn().mockRejectedValue(new Error('fallo simulado'));
      await expect(cb.execute(failingFn)).rejects.toThrow('fallo simulado');
      expect(cb.state).toBe(STATES.OPEN);

      const realNow = Date.now;
      jest.spyOn(Date, 'now').mockImplementation(() => realNow() + 30001);

      const probeFn = jest.fn().mockResolvedValue('recuperado');
      await cb.execute(probeFn);

      expect(cb.state).toBe(STATES.CLOSED);
      expect(cb.failureCount).toBe(0);
    });

    test('reabre el circuito (OPEN) si la petición de prueba en HALF_OPEN falla', async () => {
      const cb = new CircuitBreaker({ name: 'test-service', failureThreshold: 1, timeout: 30000 });
      const failingFn = jest.fn().mockRejectedValue(new Error('fallo simulado'));
      await expect(cb.execute(failingFn)).rejects.toThrow('fallo simulado');
      expect(cb.state).toBe(STATES.OPEN);

      const realNow = Date.now;
      jest.spyOn(Date, 'now').mockImplementation(() => realNow() + 30001);

      const stillFailingFn = jest.fn().mockRejectedValue(new Error('sigue caído'));
      await expect(cb.execute(stillFailingFn)).rejects.toThrow('sigue caído');

      expect(cb.state).toBe(STATES.OPEN);
    });
  });

  // ── getStatus ──────────────────────────────────────────────────────────────
  describe('getStatus', () => {
    test('retorna un snapshot consistente del estado interno', async () => {
      const cb = new CircuitBreaker({ name: 'ms-analitica', failureThreshold: 5, timeout: 30000 });
      const fn = jest.fn().mockRejectedValue(new Error('fallo simulado'));

      await expect(cb.execute(fn)).rejects.toThrow('fallo simulado');

      const status = cb.getStatus();

      expect(status.name).toBe('ms-analitica');
      expect(status.state).toBe(STATES.CLOSED);
      expect(status.failureCount).toBe(1);
      expect(status.successCount).toBe(0);
      expect(status.lastFailureTime).not.toBeNull();
    });

    test('lastFailureTime es null si nunca ha fallado', () => {
      const cb = new CircuitBreaker({ name: 'test-service' });

      expect(cb.getStatus().lastFailureTime).toBeNull();
    });
  });
});