"use client";

import { useEffect, useState } from "react";
import { createClient } from "@/lib/supabase/client";

export default function AppHandoffPage() {
  const [status, setStatus] = useState("Signing in...");

  useEffect(() => {
    async function handleHandoff() {
      const hash = window.location.hash.substring(1);
      const params = new URLSearchParams(hash);
      const accessToken = params.get("access_token");
      const refreshToken = params.get("refresh_token");

      if (!accessToken || !refreshToken) {
        setStatus("Missing authentication tokens. Please sign in.");
        setTimeout(() => {
          window.location.href = "/sign-in";
        }, 2000);
        return;
      }

      const supabase = createClient();
      const { error } = await supabase.auth.setSession({
        access_token: accessToken,
        refresh_token: refreshToken,
      });

      if (error) {
        setStatus("Session expired. Please sign in again.");
        setTimeout(() => {
          window.location.href = "/sign-in";
        }, 2000);
        return;
      }

      window.location.href = "/dashboard/credits";
    }

    handleHandoff();
  }, []);

  return (
    <div className="flex min-h-screen items-center justify-center">
      <p className="text-lg text-gray-600">{status}</p>
    </div>
  );
}
