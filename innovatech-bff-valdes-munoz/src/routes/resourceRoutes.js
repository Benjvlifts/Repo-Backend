const express = require('express');
const router = express.Router();
const { httpClient } = require('../services/httpClient');

router.get('/', async (req, res, next) => {
  try {
    const data = await httpClient.resources.getAll({ Authorization: req.headers.authorization });
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.get('/available', async (req, res, next) => {
  try {
    const data = await httpClient.resources.getAvailable({ Authorization: req.headers.authorization });
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.get('/department/:dept', async (req, res, next) => {
  try {
    const data = await httpClient.resources.getByDepartment(req.params.dept, { Authorization: req.headers.authorization });
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.get('/:id', async (req, res, next) => {
  try {
    const data = await httpClient.resources.getById(req.params.id, { Authorization: req.headers.authorization });
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.post('/', async (req, res, next) => {
  try {
    const data = await httpClient.resources.create(req.body, { Authorization: req.headers.authorization });
    res.status(201).json(data);
  } catch (err) { next(err); }
});

router.patch('/:id/availability', async (req, res, next) => {
  try {
    const data = await httpClient.resources.updateAvailability(req.params.id, req.body, { Authorization: req.headers.authorization });
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.patch('/:id/assign', async (req, res, next) => {
  try {
    const data = await httpClient.resources.assignToProject(req.params.id, req.body, { Authorization: req.headers.authorization });
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.delete('/:id', async (req, res, next) => {
  try {
    await httpClient.resources.delete(req.params.id, { Authorization: req.headers.authorization });
    res.status(204).send();
  } catch (err) { next(err); }
});

module.exports = router;