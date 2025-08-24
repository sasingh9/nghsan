import React, { useState, useEffect } from 'react';
import { DataGrid } from '@mui/x-data-grid';
import {
    Button, Typography, Box, IconButton, Dialog, DialogActions, DialogContent,
    DialogTitle, TextField, Grid, Snackbar, Alert
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';

// Helper to format date for input fields
const formatDateForInput = (dateString) => {
    if (!dateString) return '';
    // Handles both arrays (from JSON) and strings
    const date = new Date(Array.isArray(dateString) ? dateString.join('-') : dateString);
    return date.toISOString().split('T')[0];
};


const FundFormDialog = ({ open, onClose, onSave, fund }) => {
    const [formData, setFormData] = useState({});

    useEffect(() => {
        setFormData(fund || {});
    }, [fund]);

    const handleChange = (event) => {
        const { name, value } = event.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSave = () => {
        onSave(formData);
    };

    const isEditMode = fund && fund.fundID;

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>{isEditMode ? 'Edit Fund' : 'Add New Fund'}</DialogTitle>
            <DialogContent>
                <Grid container spacing={2} sx={{ mt: 1 }}>
                    {/* Key Fields */}
                    <Grid item xs={12} sm={4}>
                        <TextField label="Fund ID" name="fundID" value={formData.fundID || ''} onChange={handleChange} fullWidth disabled={isEditMode} />
                    </Grid>
                    <Grid item xs={12} sm={8}>
                        <TextField label="Fund Name" name="fundName" value={formData.fundName || ''} onChange={handleChange} fullWidth />
                    </Grid>

                    {/* Details */}
                    <Grid item xs={12} sm={4}>
                        <TextField label="Fund Ticker" name="fundTicker" value={formData.fundTicker || ''} onChange={handleChange} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField label="ISIN" name="isin" value={formData.isin || ''} onChange={handleChange} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField label="Fund Type" name="fundType" value={formData.fundType || ''} onChange={handleChange} fullWidth />
                    </Grid>

                    {/* Legal & Domicile */}
                    <Grid item xs={12} sm={6}>
                        <TextField label="Legal Structure" name="legalStructure" value={formData.legalStructure || ''} onChange={handleChange} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField label="Domicile" name="domicile" value={formData.domicile || ''} onChange={handleChange} fullWidth />
                    </Grid>

                    {/* Dates */}
                    <Grid item xs={12} sm={4}>
                        <TextField label="Inception Date" name="inceptionDate" type="date" value={formatDateForInput(formData.inceptionDate)} onChange={handleChange} InputLabelProps={{ shrink: true }} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField label="Fiscal Year End (MM-DD)" name="fiscalYearEnd" value={formData.fiscalYearEnd || ''} onChange={handleChange} fullWidth />
                    </Grid>
                     <Grid item xs={12} sm={4}>
                        <TextField label="Base Currency" name="baseCurrency" value={formData.baseCurrency || ''} onChange={handleChange} fullWidth />
                    </Grid>

                    {/* Fees */}
                    <Grid item xs={12} sm={6}>
                        <TextField label="Management Fee (%)" name="managementFee" type="number" value={formData.managementFee || ''} onChange={handleChange} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField label="Performance Fee (%)" name="performanceFee" type="number" value={formData.performanceFee || ''} onChange={handleChange} fullWidth />
                    </Grid>

                    {/* Parties */}
                    <Grid item xs={12} sm={6}>
                        <TextField label="Fund Administrator" name="fundAdministrator" value={formData.fundAdministrator || ''} onChange={handleChange} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField label="Custodian" name="custodian" value={formData.custodian || ''} onChange={handleChange} fullWidth />
                    </Grid>
                    <Grid item xs={12}>
                        <TextField label="Prime Brokers (comma-separated)" name="primeBrokers" value={formData.primeBrokers || ''} onChange={handleChange} fullWidth />
                    </Grid>

                    {/* Strategy & Cycles */}
                    <Grid item xs={12}>
                        <TextField label="Investment Strategy" name="investmentStrategy" multiline rows={3} value={formData.investmentStrategy || ''} onChange={handleChange} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField label="Valuation Frequency" name="valuationFrequency" value={formData.valuationFrequency || ''} onChange={handleChange} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField label="Subscription Cycle" name="subscriptionCycle" value={formData.subscriptionCycle || ''} onChange={handleChange} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField label="Redemption Cycle" name="redemptionCycle" value={formData.redemptionCycle || ''} onChange={handleChange} fullWidth />
                    </Grid>

                    {/* NAV */}
                    <Grid item xs={12} sm={4}>
                        <TextField label="Latest NAV" name="nav" type="number" value={formData.nav || ''} onChange={handleChange} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField label="NAV Date" name="navDate" type="date" value={formatDateForInput(formData.navDate)} onChange={handleChange} InputLabelProps={{ shrink: true }} fullWidth />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField label="Status" name="status" value={formData.status || ''} onChange={handleChange} fullWidth />
                    </Grid>
                </Grid>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancel</Button>
                <Button onClick={handleSave} variant="contained">Save</Button>
            </DialogActions>
        </Dialog>
    );
};


const FundMaster = () => {
    const [funds, setFunds] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [currentFund, setCurrentFund] = useState(null);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });


    const fetchFunds = async () => {
        setLoading(true);
        try {
            const response = await fetch('/api/funds', { credentials: 'include' });
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const data = await response.json();
            setFunds(data);
            setError(null);
        } catch (err) {
            const errorMsg = err.message || 'Failed to fetch funds.';
            setError(errorMsg);
            setSnackbar({ open: true, message: errorMsg, severity: 'error' });
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchFunds();
    }, []);

    const handleOpenDialog = (fund = null) => {
        setCurrentFund(fund);
        setDialogOpen(true);
    };

    const handleCloseDialog = () => {
        setDialogOpen(false);
        setCurrentFund(null);
    };

    const handleSave = async (fundData) => {
        const isEdit = !!currentFund;
        const url = isEdit ? `/api/funds/${fundData.fundID}` : '/api/funds';
        const method = isEdit ? 'PUT' : 'POST';

        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(fundData),
                credentials: 'include',
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'An unknown error occurred.' }));
                throw new Error(errorData.message || `Request failed with status ${response.status}`);
            }

            setSnackbar({ open: true, message: `Fund ${isEdit ? 'updated' : 'created'} successfully!`, severity: 'success' });
            fetchFunds(); // Refresh data
        } catch (err) {
            setSnackbar({ open: true, message: `Error: ${err.message}`, severity: 'error' });
        } finally {
            handleCloseDialog();
        }
    };

    const handleDelete = async (id) => {
        if (window.confirm('Are you sure you want to delete this fund?')) {
            try {
                const response = await fetch(`/api/funds/${id}`, {
                    method: 'DELETE',
                    credentials: 'include',
                });

                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({ message: 'An unknown error occurred.' }));
                    throw new Error(errorData.message || `Request failed with status ${response.status}`);
                }

                setSnackbar({ open: true, message: 'Fund deleted successfully!', severity: 'success' });
                fetchFunds(); // Refresh data
            } catch (err) {
                setSnackbar({ open: true, message: `Error: ${err.message}`, severity: 'error' });
            }
        }
    };

    const handleCloseSnackbar = (event, reason) => {
        if (reason === 'clickaway') {
            return;
        }
        setSnackbar({ ...snackbar, open: false });
    };


    const columns = [
        { field: 'fundID', headerName: 'Fund ID', width: 150 },
        { field: 'fundName', headerName: 'Fund Name', width: 250 },
        { field: 'fundTicker', headerName: 'Ticker', width: 120 },
        { field: 'isin', headerName: 'ISIN', width: 150 },
        { field: 'fundType', headerName: 'Fund Type', width: 150 },
        { field: 'status', headerName: 'Status', width: 120 },
        {
            field: 'actions',
            headerName: 'Actions',
            width: 120,
            sortable: false,
            renderCell: (params) => (
                <>
                    <IconButton onClick={() => handleOpenDialog(params.row)} size="small">
                        <EditIcon />
                    </IconButton>
                    <IconButton onClick={() => handleDelete(params.row.fundID)} size="small">
                        <DeleteIcon />
                    </IconButton>
                </>
            ),
        },
    ];

    return (
        <div>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h4" gutterBottom>
                    Fund Master
                </Typography>
                <Button variant="contained" color="primary" onClick={() => handleOpenDialog()}>
                    Add Fund
                </Button>
            </Box>

            <div style={{ height: 650, width: '100%' }}>
                <DataGrid
                    rows={funds}
                    columns={columns}
                    pageSize={10}
                    rowsPerPageOptions={[10, 25, 50]}
                    loading={loading}
                    getRowId={(row) => row.fundID}
                    slots={{
                      noRowsOverlay: () => (
                        <Box sx={{ p: 2, textAlign: 'center' }}>
                          <Typography>No funds found. Use the "Add Fund" button to create one.</Typography>
                        </Box>
                      ),
                    }}
                />
            </div>

            <FundFormDialog
                open={dialogOpen}
                onClose={handleCloseDialog}
                onSave={handleSave}
                fund={currentFund}
            />

            <Snackbar open={snackbar.open} autoHideDuration={6000} onClose={handleCloseSnackbar}>
                <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </div>
    );
};

export default FundMaster;
