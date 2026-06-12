import Navbar from './Navbar';
import AuthPrompt from './AuthPrompt';
import { Outlet } from 'react-router-dom';

export default function Layout() {
  return (
    <>
      <Navbar />
      <main>
        <Outlet />
      </main>
      <AuthPrompt />
    </>
  );
}
