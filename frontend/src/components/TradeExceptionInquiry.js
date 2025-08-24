import React, { useState } from 'react';
import { DataGrid } from '@mui/x-data-grid';
import { Button, TextField, Typography, Box, Modal, Paper } from '@mui/material';

const safeFormatJson = (jsonString) => {
    if (!jsonString) return 'No JSON data available.';
    try {
        const parsed = JSON.parse(jsonString);
        return JSON.stringify(parsed, null, 2);
    } catch (error) {
        return jsonString; // Return raw string if parsing fails
    }
};

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

function TradeExceptionInquiry() {
    const [clientRef, setClientRef] = useState('');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [exceptions, setExceptions] = useState([]);
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
        { field: 'id', headerName: 'ID', width: 100 },
        { field: 'clientReferenceNumber', headerName: 'Client Reference', width: 200 },
        { field: 'failureReason', headerName: 'Failure Reason', width: 300 },
        { field: 'createdAt', headerName: 'Created At', width: 200, valueGetter: (value) => new Date(value).toLocaleString() },
        {
            field: 'failedTradeJson',
            headerName: 'Failed JSON',
            width: 150,
            renderCell: (params) => (
                <Button variant="outlined" onClick={() => handleOpen(params.value)}>
                    ...
                </Button>
            ),
        },
    ];

    const handleSubmit = async (event) => {
        event.preventDefault();
        const hasClientRef = clientRef.trim();
        const hasDateRange = startDate && endDate;

        if (!hasClientRef && !hasDateRange) {
            setError('Please enter a Client Reference Number or select a date range.');
            return;
        }
        if (hasDateRange && new Date(startDate) >= new Date(endDate)) {
            setError('Start date must be before end date.');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const params = new URLSearchParams();
            if (hasClientRef) {
                params.append('clientReferenceNumber', clientRef);
            }
            if (hasDateRange) {
                params.append('startDate', new Date(startDate).toISOString().slice(0, 16));
                params.append('endDate', new Date(endDate).toISOString().slice(0, 16));
            }

            const response = await fetch(`/api/exceptions?${params.toString()}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });
            const data = await response.json();
            if (response.ok && data.success) {
                setExceptions(data.data);
            } else {
                throw new Error(data.message || `HTTP error! status: ${response.status}`);
            }
        } catch (e) {
            setError('Failed to fetch trade exceptions. ' + e.message);
            setExceptions([]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <Typography variant="h4" gutterBottom>Trade Exception Inquiry</Typography>
            <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 2 }}>
                <TextField
                    label="Client Reference Number"
                    value={clientRef}
                    onChange={(e) => setClientRef(e.target.value)}
                    variant="outlined"
                    size="small"
                    sx={{ minWidth: '240px' }}
                />
                <TextField
                    label="Start Date"
                    type="datetime-local"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                    size="small"
                />
                <TextField
                    label="End Date"
                    type="datetime-local"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                    size="small"
                />
                <Button type="submit" disabled={loading} variant="contained">
                    {loading ? 'Loading...' : 'Search'}
                </Button>
            </Box>

            {error && <Typography color="error">{error}</Typography>}

            <div style={{ height: 600, width: '100%' }}>
                <DataGrid
                    rows={exceptions}
                    columns={columns}
                    pageSize={10}
                    rowsPerPageOptions={[10]}
                    loading={loading}
                    getRowId={(row) => row.id}
                    slots={{
                      noRowsOverlay: () => (
                        <Box sx={{ p: 2, textAlign: 'center' }}>
                          <Typography>No exceptions found.</Typography>
                        </Box>
                      ),
                    }}
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
                        Failed Trade JSON
                    </Typography>
                    <pre id="modal-description" style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                        {safeFormatJson(selectedJson)}
                    </pre>
                </Paper>
            </Modal>
        </div>
    );
}

export default TradeExceptionInquiry;
