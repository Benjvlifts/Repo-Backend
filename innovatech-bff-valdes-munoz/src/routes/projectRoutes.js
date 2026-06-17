const express = require('express');
const router = express.Router();
const { httpClient } = require('../services/httpClient');

// ── Helpers ──────────────────────────────────────────────────────────────────

const authHeader = (req) => ({ Authorization: req.headers.authorization });

// ── CRUD básico ───────────────────────────────────────────────────────────────

router.get('/', async (req, res, next) => {
  try {
    const data = await httpClient.projects.getAll(req.query, authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.get('/:id', async (req, res, next) => {
  try {
    const data = await httpClient.projects.getById(req.params.id, authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.post('/', async (req, res, next) => {
  try {
    const data = await httpClient.projects.create(req.body, authHeader(req));
    res.status(201).json(data);
  } catch (err) { next(err); }
});

// PUT /api/projects/:id — edición completa (Admin)
router.put('/:id', async (req, res, next) => {
  try {
    const data = await httpClient.projects.update(req.params.id, req.body, authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

// PATCH /api/projects/:id/status — cambiar estado (Admin)
router.patch('/:id/status', async (req, res, next) => {
  try {
    const data = await httpClient.projects.updateStatus(req.params.id, req.body, authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.delete('/:id', async (req, res, next) => {
  try {
    await httpClient.projects.delete(req.params.id, authHeader(req));
    res.status(204).send();
  } catch (err) { next(err); }
});

// ── Asignación de empleado (Admin + Manager) ─────────────────────────────────

// PATCH /api/projects/:id/assign — asignar empleado
router.patch('/:id/assign', async (req, res, next) => {
  try {
    const data = await httpClient.projects.assignEmployee(req.params.id, req.body, authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

// DELETE /api/projects/:id/assign — desasignar empleado
router.delete('/:id/assign', async (req, res, next) => {
  try {
    const data = await httpClient.projects.unassignEmployee(req.params.id, authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

// ── Notas de avance (tipo PR de GitHub) ──────────────────────────────────────

// GET /api/projects/:id/notes — obtener notas
router.get('/:id/notes', async (req, res, next) => {
  try {
    const data = await httpClient.projects.getNotes(req.params.id, authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

// POST /api/projects/:id/notes — agregar nota (Employee)
router.post('/:id/notes', async (req, res, next) => {
  try {
    const data = await httpClient.projects.addNote(req.params.id, req.body, authHeader(req));
    res.status(201).json(data);
  } catch (err) { next(err); }
});

// PATCH /api/projects/:id/notes/:noteId/review — revisar nota (Admin + Manager)
router.patch('/:id/notes/:noteId/review', async (req, res, next) => {
  try {
    const data = await httpClient.projects.reviewNote(
      req.params.id, req.params.noteId, req.body, authHeader(req)
    );
    res.status(200).json(data);
  } catch (err) { next(err); }
});

module.exports = router;