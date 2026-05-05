import Navbar from './Navbar'
import { Outlet } from 'react-router-dom'

const LeLayout = () => {
  return (
    <>
      <Navbar />
      <main>
        <Outlet />
      </main>
    </> 
  )
}

export default LeLayout
