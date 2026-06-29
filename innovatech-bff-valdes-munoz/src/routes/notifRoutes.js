const express = require('express');
const router = express.Router();
const { httpClient } = require('../services/httpClient');

const authHeader = (req) => ({ Authorization: req.headers.authorization });

router.get('/proyecto/:projectId', async (req, res, next) => {
  try {
    const data = await httpClient.notificaciones.getByProject(req.params.projectId, authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.get('/proyecto/:projectId/no-leidas', async (req, res, next) => {
  try {
    const data = await httpClient.notificaciones.getUnread(req.params.projectId, authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.patch('/:id/read', async (req, res, next) => {
  try {
    await httpClient.notificaciones.markAsRead(req.params.id, authHeader(req));
    res.status(204).send();
  } catch (err) { next(err); }
});

module.exports = router;