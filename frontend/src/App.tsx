import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import Landing from '@/pages/Landing'
import Login from '@/pages/Login'
import Signup from '@/pages/Signup'
import Dashboard from '@/pages/Dashboard'
import Pipelines from '@/pages/Pipelines'
import PipelineDetail from '@/pages/PipelineDetail'
import Executions from '@/pages/Executions'
import ExecutionDetail from '@/pages/ExecutionDetail'
import Workers from '@/pages/Workers'
import Logs from '@/pages/Logs'
import Metrics from '@/pages/Metrics'
import Settings from '@/pages/Settings'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/pipelines" element={<Pipelines />} />
          <Route path="/pipelines/:id" element={<PipelineDetail />} />
          <Route path="/executions" element={<Executions />} />
          <Route path="/executions/:id" element={<ExecutionDetail />} />
          <Route path="/workers" element={<Workers />} />
          <Route path="/logs" element={<Logs />} />
          <Route path="/metrics" element={<Metrics />} />
          <Route path="/settings" element={<Settings />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
