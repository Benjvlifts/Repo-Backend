const STATES = {
  CLOSED: 'CLOSED',
  OPEN: 'OPEN',
  HALF_OPEN: 'HALF_OPEN',
};

class CircuitBreaker {
  constructor({ name, failureThreshold = 5, timeout = 30000 } = {}) {
    this.name = name || 'unknown';
    this.failureThreshold = failureThreshold;
    this.timeout = timeout;
    this.state = STATES.CLOSED;
    this.failureCount = 0;
    this.lastFailureTime = null;
    this.successCount = 0;
  }

  async execute(fn, fallback = null) {
    if (this.state === STATES.OPEN) {
      if (this._shouldAttemptReset()) {
        this.state = STATES.HALF_OPEN;
        console.log(`[CircuitBreaker:${this.name}] → HALF_OPEN, probando recuperación`);
      } else {
        console.warn(`[CircuitBreaker:${this.name}] ABIERTO - rechazando solicitud`);
        if (fallback) return fallback();
        const error = new Error(`Servicio ${this.name} no disponible (circuit open)`);
        error.statusCode = 503;
        throw error;
      }
    }

    try {
      const result = await fn();
      this._onSuccess();
      return result;
    } catch (err) {
      this._onFailure();
      if (fallback) return fallback();
      throw err;
    }
  }

  _onSuccess() {
    this.failureCount = 0;
    this.successCount++;
    if (this.state === STATES.HALF_OPEN) {
      this.state = STATES.CLOSED;
      console.log(`[CircuitBreaker:${this.name}] → CLOSED (recuperado)`);
    }
  }

  _onFailure() {
    this.failureCount++;
    this.lastFailureTime = Date.now();
    console.warn(`[CircuitBreaker:${this.name}] fallo ${this.failureCount}/${this.failureThreshold}`);
    if (this.failureCount >= this.failureThreshold) {
      this.state = STATES.OPEN;
      console.error(`[CircuitBreaker:${this.name}] → OPEN`);
    }
  }

  _shouldAttemptReset() {
    return this.lastFailureTime && (Date.now() - this.lastFailureTime) >= this.timeout;
  }

  getStatus() {
    return {
      name: this.name,
      state: this.state,
      failureCount: this.failureCount,
      successCount: this.successCount,
      lastFailureTime: this.lastFailureTime ? new Date(this.lastFailureTime).toISOString() : null,
    };
  }
}

module.exports = { CircuitBreaker, STATES };