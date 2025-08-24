import React, { useState } from 'react';
import axios from 'axios';
import { DataGrid } from '@mui/x-data-grid';
import { Button, TextField, Typography, Box, Modal, Paper } from '@mui/material';

const style = {
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 600,
    bgcolor: 'background.paper',
    border: '2px solid #000',
    boxShadow: 24,
    p: 4,
};

const JsonDataViewer = () => {
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [open, setOpen] = useState(false);
    const [selectedJson, setSelectedJson] = useState(null);

    const handleOpen = (json) => {
        setSelectedJson(json);
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        setSelectedJson(null);
    };

    const columns = [
        { field: 'id', headerName: 'Record ID', width: 150 },
        { field: 'messageKey', headerName: 'Message Key', width: 250 },
        { field: 'createdAt', headerName: 'Created At', width: 200, valueGetter: (value) => new Date(value).toLocaleString() },
        {
            field: 'jsonData',
            headerName: 'JSON Data',
            width: 150,
            renderCell: (params) => (
                <Button variant="outlined" onClick={() => handleOpen(params.value)}>
                    ...
                </Button>
            ),
        },
    ];

    const fetchData = async () => {
        setLoading(true);
        setError('');
        setData([]);

        if (!startDate || !endDate) {
            setError('Please select both a start and end date.');
            setLoading(false);
            return;
        }

        try {
            const params = {
                startDate: new Date(startDate).toISOString(),
                endDate: new Date(endDate).toISOString(),
            };
            const response = await axios.get('/api/data', { params });
            if (response.data && response.data.success) {
                setData(response.data.data.content);
            } else {
                setError(response.data.message || 'Failed to fetch data.');
            }
        } catch (err) {
            setError('Failed to fetch data. Make sure the backend is running.');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <Typography variant="h4" gutterBottom>Inbound Message</Typography>
            <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <TextField
                    label="Start Date"
                    type="datetime-local"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                />
                <TextField
                    label="End Date"
                    type="datetime-local"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                />
                <Button onClick={fetchData} disabled={loading} variant="contained">
                    {loading ? 'Loading...' : 'Fetch Data'}
                </Button>
            </Box>
            {error && <Typography color="error">{error}</Typography>}
            <div style={{ height: 600, width: '100%' }}>
                <DataGrid
                    rows={data}
                    columns={columns}
                    pageSize={10}
                    rowsPerPageOptions={[10]}
                    loading={loading}
                />
            </div>
            <Modal
                open={open}
                onClose={handleClose}
                aria-labelledby="modal-title"
                aria-describedby="modal-description"
            >
                <Paper sx={style}>
                    <Typography id="modal-title" variant="h6" component="h2">
                        JSON Data
                    </Typography>
                    <pre id="modal-description" style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                        {selectedJson ? JSON.stringify(JSON.parse(selectedJson), null, 2) : ''}
                    </pre>
                </Paper>
            </Modal>
        </div>
    );
};

export default JsonDataViewer;
