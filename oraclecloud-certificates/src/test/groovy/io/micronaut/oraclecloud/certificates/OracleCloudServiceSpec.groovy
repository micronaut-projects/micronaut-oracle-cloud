package io.micronaut.oraclecloud.certificates

import com.oracle.bmc.certificates.Certificates
import com.oracle.bmc.certificates.model.CertificateBundleWithPrivateKey
import com.oracle.bmc.certificates.model.Validity
import com.oracle.bmc.certificates.responses.GetCertificateBundleResponse
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.oraclecloud.certificates.events.CertificateEvent
import io.micronaut.oraclecloud.certificates.services.OracleCloudCertificateService
import spock.lang.Specification

class OracleCloudServiceSpec extends Specification {

    public static String PRIVATE_KEY = """-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDaz3fk1bp7xmAY
7wRYdu6998ioDEkwemwvr3/8OR5Ij+56tGcOgSAu7HALtcGHYB91NCO3UlDgnauB
Z5Crm7HMFdBLUCxfBoxn4s+oNO5EocPbPqrun++V5FMcXCmj5FagO+TshSzyBl/Q
MC1SqnIiQ7VUlO/Zhsr2q4DKiCz1v6eT5WbFly8eH36M3//q7sb2qFEvOUaj4Nhj
G+LdbQll04EGUPhIPeBkuWS714EDjKtUv35+Ncvd285q6MfPihku3mLQRLD6nHT9
7mPi0VMnOp1A3VTDnTx7kjXGJsY9oSE2X0POxACWnyV5a61HqAa8TpE4h2MBksI9
nuRFz7dxAgMBAAECggEAGX/jG4Zy2JjaOx2jtoGJuhbFyhvAbcdek0ITGrw3VMJ9
Ssx6VBzlOMKyHhM87f9cOybr4KHVrg+B3K9Kk00uL7f9EcHSofJb64FprNMaT2JA
tmy7s9psq92zd9MfwStLkxnXyF6OydfHU7ZBmegmK+sTFzvSJdoJDi0XccLj5nGL
sEtmYlcIb++ikF8d+EyKpCS0woAYw9r8WIk0NyX7z+6ceLiC0RWtafCDp5X/o7oF
q2MJNjmlJGHqLVGViYUI/L4RKlzXVx5i7fklBbA08hyoth8H2cnJeM+Cfqa3NhRn
ZrL9p1K34sOnkdaRan9PaiC3UH21Dp6J5yYGXxBkvwKBgQDzy41v7UfDxsv9QBGY
2sV4UxtZUHJXzqTdrZdkxAX+Z/RI5c5s995U/x7DCjjKBrFuLB52pe1MGm/nb5Dz
L006VXF2monqGOyQmI1uSqdODdGSD8rLA2sPCKMUYB5+f0pBgrAiVYeoGOW3h6X8
5GNjyZ3jn0XIDi1bbH/sNZFVkwKBgQDlw7cNF+o7wx7L9oXSqU73s5x9nsLNHVd1
QM2n3l6Dgu1vOIbZoKJdRWUVB1siH4Jq4aVigSfjxA5zJlJ9ON0BpjlBqyEJN3iN
DPhqD6asAyZ7M8M3fCGk341t+9Im5uStBJonJP2O02FdROCleaaumQ5y7wwGGXhj
qOrll3MhawKBgGNdKEVhKWnC4atTbJinOerDvJbXcfMV1x4+vUCloGfDSM5ZU5wY
Hnb4EDqgNOsqdTCZLMVg9WmaMNfcIfDr64cGAhu7+s/93pVEiAhWxv/KJAtrAlVi
DEVxWL5aOrOF/+bZdB5aS9MYarA1ylJbZ6LpLr/yT4etN5FWlIDaiGSfAoGBAN1U
UhIX1nhpNlJuEG1k0QAFkhrkg2yI9kWp/jHWIJ940PXRsv0TIzTL81p1cpGFXuFM
qj8ggWeD5hOAd/fOff7nu8IJclJpkAP32Zh5qSmMA1as/0GEnvgurROkf8UfqGsO
wtwwYtxhvSnWfryIWktYfzWyFpgNkw4Vmuk9ohcvAoGBAK74NeUJthS+l4fyHwSI
/17Prf2kg4GDZMJbAoGORPQ5luqRGnVfYicfyboi1Z8nYmwnFsonsiHycU7Sb8wp
4J8PbZocWFVw+UM/6p09koPvPrejVDde/EW+eFu6QH10pS7dOLKUSTDn8ou/wkA+
BacFNyeD1OERw6rlzg8pA0YI
-----END PRIVATE KEY-----"""

    public static String CERTIFICATE_STRING = """-----BEGIN CERTIFICATE-----
MIIFajCCBFKgAwIBAgIUdqm3iigytoj0A4qjzeqqcyUUiQQwDQYJKoZIhvcNAQEL
BQAwEDEOMAwGA1UEAwwFdGVzdDEwHhcNMjMwNTMwMTcyMDQyWhcNMjMwODE1MDAw
MDAwWjAUMRIwEAYDVQQDDAlNaWNyb25hdXQwggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQDaz3fk1bp7xmAY7wRYdu6998ioDEkwemwvr3/8OR5Ij+56tGcO
gSAu7HALtcGHYB91NCO3UlDgnauBZ5Crm7HMFdBLUCxfBoxn4s+oNO5EocPbPqru
n++V5FMcXCmj5FagO+TshSzyBl/QMC1SqnIiQ7VUlO/Zhsr2q4DKiCz1v6eT5WbF
ly8eH36M3//q7sb2qFEvOUaj4NhjG+LdbQll04EGUPhIPeBkuWS714EDjKtUv35+
Ncvd285q6MfPihku3mLQRLD6nHT97mPi0VMnOp1A3VTDnTx7kjXGJsY9oSE2X0PO
xACWnyV5a61HqAa8TpE4h2MBksI9nuRFz7dxAgMBAAGjggK2MIICsjAOBgNVHQ8B
Af8EBAMCBaAwDAYDVR0TAQH/BAIwADAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAgYI
KwYBBQUHAwEwggEzBgNVHQ4EggEqBIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEP
ADCCAQoCggEBANrPd+TVunvGYBjvBFh27r33yKgMSTB6bC+vf/w5HkiP7nq0Zw6B
IC7scAu1wYdgH3U0I7dSUOCdq4FnkKubscwV0EtQLF8GjGfiz6g07kShw9s+qu6f
75XkUxxcKaPkVqA75OyFLPIGX9AwLVKqciJDtVSU79mGyvargMqILPW/p5PlZsWX
Lx4ffozf/+ruxvaoUS85RqPg2GMb4t1tCWXTgQZQ+Eg94GS5ZLvXgQOMq1S/fn41
y93bzmrox8+KGS7eYtBEsPqcdP3uY+LRUyc6nUDdVMOdPHuSNcYmxj2hITZfQ87E
AJafJXlrrUeoBrxOkTiHYwGSwj2e5EXPt3ECAwEAATCCATcGA1UdIwSCAS4wggEq
gIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANHglJHO6O56lMIp
DUCff+vMWhukXlnZTq4kS7wpiJdMSTYFzN8kOKLDRujLneAfyrYDW9Tm4xFYBZRX
uRiCq7H7ck+HFR0Xq5fuKzp5j/KaGxMbloSGOWqdcWvHOSMwAr4mmGui4le14nXI
fUYZcZuXwooEHSHOMkVrZUa/6M0lNNoTWvUCj74HAXJNAEu7D+hfcKOQ0E7WbSYR
Yol+Kjw3ij6HTGPIjUpY8emDd4tWkJx3pzuTUIABCAcqz9al/Ok3ac2ZEPQbM83N
4JbWrioj+ic8nkA+xvd8NYmdbfU9fiuUXOeQQMmUeAqlgmE3nC+zfSihLYaLJ61A
skTL7a0CAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAIswvhJX2y0YZiiFJy5b6KPID
SJuD+UZmIiDs61ZL6IK3iNRMnJX0IgTQMi5Ema1svvFCMM6502fMm5ktDSEr/GXY
KvqeJ4VmcgfCAxSE5tDek5/KpKOw1X57jqdoMJ25xLWTneN4YeTJDeTAG56T5/Ci
nzsQ7KiJuJc/zsKCgscZ/bQocbz3Pn9jPZTBktCdqsCvVOqHB0RuaclQxWfKsRN4
52Xov7EGYbm0xfifdg2HLnZFUzOnVAEQNsnKXW095IGFgUYlonuQ+jotRfLEf3hm
IgQuEdz+6WvdabYC1igIWN9od6fnoNI3NSRwuttvnJVWX4FkVnhu1YRdGdNkGg==
-----END CERTIFICATE-----"""

    public static String CERTIFICATE_CHAIN_STRING = """-----BEGIN CERTIFICATE-----
MIIFajCCBFKgAwIBAgIUdqm3iigytoj0A4qjzeqqcyUUiQQwDQYJKoZIhvcNAQEL
BQAwEDEOMAwGA1UEAwwFdGVzdDEwHhcNMjMwNTMwMTcyMDQyWhcNMjMwODE1MDAw
MDAwWjAUMRIwEAYDVQQDDAlNaWNyb25hdXQwggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQDaz3fk1bp7xmAY7wRYdu6998ioDEkwemwvr3/8OR5Ij+56tGcO
gSAu7HALtcGHYB91NCO3UlDgnauBZ5Crm7HMFdBLUCxfBoxn4s+oNO5EocPbPqru
n++V5FMcXCmj5FagO+TshSzyBl/QMC1SqnIiQ7VUlO/Zhsr2q4DKiCz1v6eT5WbF
ly8eH36M3//q7sb2qFEvOUaj4NhjG+LdbQll04EGUPhIPeBkuWS714EDjKtUv35+
Ncvd285q6MfPihku3mLQRLD6nHT97mPi0VMnOp1A3VTDnTx7kjXGJsY9oSE2X0PO
xACWnyV5a61HqAa8TpE4h2MBksI9nuRFz7dxAgMBAAGjggK2MIICsjAOBgNVHQ8B
Af8EBAMCBaAwDAYDVR0TAQH/BAIwADAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAgYI
KwYBBQUHAwEwggEzBgNVHQ4EggEqBIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEP
ADCCAQoCggEBANrPd+TVunvGYBjvBFh27r33yKgMSTB6bC+vf/w5HkiP7nq0Zw6B
IC7scAu1wYdgH3U0I7dSUOCdq4FnkKubscwV0EtQLF8GjGfiz6g07kShw9s+qu6f
75XkUxxcKaPkVqA75OyFLPIGX9AwLVKqciJDtVSU79mGyvargMqILPW/p5PlZsWX
Lx4ffozf/+ruxvaoUS85RqPg2GMb4t1tCWXTgQZQ+Eg94GS5ZLvXgQOMq1S/fn41
y93bzmrox8+KGS7eYtBEsPqcdP3uY+LRUyc6nUDdVMOdPHuSNcYmxj2hITZfQ87E
AJafJXlrrUeoBrxOkTiHYwGSwj2e5EXPt3ECAwEAATCCATcGA1UdIwSCAS4wggEq
gIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANHglJHO6O56lMIp
DUCff+vMWhukXlnZTq4kS7wpiJdMSTYFzN8kOKLDRujLneAfyrYDW9Tm4xFYBZRX
uRiCq7H7ck+HFR0Xq5fuKzp5j/KaGxMbloSGOWqdcWvHOSMwAr4mmGui4le14nXI
fUYZcZuXwooEHSHOMkVrZUa/6M0lNNoTWvUCj74HAXJNAEu7D+hfcKOQ0E7WbSYR
Yol+Kjw3ij6HTGPIjUpY8emDd4tWkJx3pzuTUIABCAcqz9al/Ok3ac2ZEPQbM83N
4JbWrioj+ic8nkA+xvd8NYmdbfU9fiuUXOeQQMmUeAqlgmE3nC+zfSihLYaLJ61A
skTL7a0CAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAIswvhJX2y0YZiiFJy5b6KPID
SJuD+UZmIiDs61ZL6IK3iNRMnJX0IgTQMi5Ema1svvFCMM6502fMm5ktDSEr/GXY
KvqeJ4VmcgfCAxSE5tDek5/KpKOw1X57jqdoMJ25xLWTneN4YeTJDeTAG56T5/Ci
nzsQ7KiJuJc/zsKCgscZ/bQocbz3Pn9jPZTBktCdqsCvVOqHB0RuaclQxWfKsRN4
52Xov7EGYbm0xfifdg2HLnZFUzOnVAEQNsnKXW095IGFgUYlonuQ+jotRfLEf3hm
IgQuEdz+6WvdabYC1igIWN9od6fnoNI3NSRwuttvnJVWX4FkVnhu1YRdGdNkGg==
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
MIIFajCCBFKgAwIBAgIUdqm3iigytoj0A4qjzeqqcyUUiQQwDQYJKoZIhvcNAQEL
BQAwEDEOMAwGA1UEAwwFdGVzdDEwHhcNMjMwNTMwMTcyMDQyWhcNMjMwODE1MDAw
MDAwWjAUMRIwEAYDVQQDDAlNaWNyb25hdXQwggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQDaz3fk1bp7xmAY7wRYdu6998ioDEkwemwvr3/8OR5Ij+56tGcO
gSAu7HALtcGHYB91NCO3UlDgnauBZ5Crm7HMFdBLUCxfBoxn4s+oNO5EocPbPqru
n++V5FMcXCmj5FagO+TshSzyBl/QMC1SqnIiQ7VUlO/Zhsr2q4DKiCz1v6eT5WbF
ly8eH36M3//q7sb2qFEvOUaj4NhjG+LdbQll04EGUPhIPeBkuWS714EDjKtUv35+
Ncvd285q6MfPihku3mLQRLD6nHT97mPi0VMnOp1A3VTDnTx7kjXGJsY9oSE2X0PO
xACWnyV5a61HqAa8TpE4h2MBksI9nuRFz7dxAgMBAAGjggK2MIICsjAOBgNVHQ8B
Af8EBAMCBaAwDAYDVR0TAQH/BAIwADAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAgYI
KwYBBQUHAwEwggEzBgNVHQ4EggEqBIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEP
ADCCAQoCggEBANrPd+TVunvGYBjvBFh27r33yKgMSTB6bC+vf/w5HkiP7nq0Zw6B
IC7scAu1wYdgH3U0I7dSUOCdq4FnkKubscwV0EtQLF8GjGfiz6g07kShw9s+qu6f
75XkUxxcKaPkVqA75OyFLPIGX9AwLVKqciJDtVSU79mGyvargMqILPW/p5PlZsWX
Lx4ffozf/+ruxvaoUS85RqPg2GMb4t1tCWXTgQZQ+Eg94GS5ZLvXgQOMq1S/fn41
y93bzmrox8+KGS7eYtBEsPqcdP3uY+LRUyc6nUDdVMOdPHuSNcYmxj2hITZfQ87E
AJafJXlrrUeoBrxOkTiHYwGSwj2e5EXPt3ECAwEAATCCATcGA1UdIwSCAS4wggEq
gIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANHglJHO6O56lMIp
DUCff+vMWhukXlnZTq4kS7wpiJdMSTYFzN8kOKLDRujLneAfyrYDW9Tm4xFYBZRX
uRiCq7H7ck+HFR0Xq5fuKzp5j/KaGxMbloSGOWqdcWvHOSMwAr4mmGui4le14nXI
fUYZcZuXwooEHSHOMkVrZUa/6M0lNNoTWvUCj74HAXJNAEu7D+hfcKOQ0E7WbSYR
Yol+Kjw3ij6HTGPIjUpY8emDd4tWkJx3pzuTUIABCAcqz9al/Ok3ac2ZEPQbM83N
4JbWrioj+ic8nkA+xvd8NYmdbfU9fiuUXOeQQMmUeAqlgmE3nC+zfSihLYaLJ61A
skTL7a0CAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAIswvhJX2y0YZiiFJy5b6KPID
SJuD+UZmIiDs61ZL6IK3iNRMnJX0IgTQMi5Ema1svvFCMM6502fMm5ktDSEr/GXY
KvqeJ4VmcgfCAxSE5tDek5/KpKOw1X57jqdoMJ25xLWTneN4YeTJDeTAG56T5/Ci
nzsQ7KiJuJc/zsKCgscZ/bQocbz3Pn9jPZTBktCdqsCvVOqHB0RuaclQxWfKsRN4
52Xov7EGYbm0xfifdg2HLnZFUzOnVAEQNsnKXW095IGFgUYlonuQ+jotRfLEf3hm
IgQuEdz+6WvdabYC1igIWN9od6fnoNI3NSRwuttvnJVWX4FkVnhu1YRdGdNkGg==
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
MIIFajCCBFKgAwIBAgIUdqm3iigytoj0A4qjzeqqcyUUiQQwDQYJKoZIhvcNAQEL
BQAwEDEOMAwGA1UEAwwFdGVzdDEwHhcNMjMwNTMwMTcyMDQyWhcNMjMwODE1MDAw
MDAwWjAUMRIwEAYDVQQDDAlNaWNyb25hdXQwggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQDaz3fk1bp7xmAY7wRYdu6998ioDEkwemwvr3/8OR5Ij+56tGcO
gSAu7HALtcGHYB91NCO3UlDgnauBZ5Crm7HMFdBLUCxfBoxn4s+oNO5EocPbPqru
n++V5FMcXCmj5FagO+TshSzyBl/QMC1SqnIiQ7VUlO/Zhsr2q4DKiCz1v6eT5WbF
ly8eH36M3//q7sb2qFEvOUaj4NhjG+LdbQll04EGUPhIPeBkuWS714EDjKtUv35+
Ncvd285q6MfPihku3mLQRLD6nHT97mPi0VMnOp1A3VTDnTx7kjXGJsY9oSE2X0PO
xACWnyV5a61HqAa8TpE4h2MBksI9nuRFz7dxAgMBAAGjggK2MIICsjAOBgNVHQ8B
Af8EBAMCBaAwDAYDVR0TAQH/BAIwADAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAgYI
KwYBBQUHAwEwggEzBgNVHQ4EggEqBIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEP
ADCCAQoCggEBANrPd+TVunvGYBjvBFh27r33yKgMSTB6bC+vf/w5HkiP7nq0Zw6B
IC7scAu1wYdgH3U0I7dSUOCdq4FnkKubscwV0EtQLF8GjGfiz6g07kShw9s+qu6f
75XkUxxcKaPkVqA75OyFLPIGX9AwLVKqciJDtVSU79mGyvargMqILPW/p5PlZsWX
Lx4ffozf/+ruxvaoUS85RqPg2GMb4t1tCWXTgQZQ+Eg94GS5ZLvXgQOMq1S/fn41
y93bzmrox8+KGS7eYtBEsPqcdP3uY+LRUyc6nUDdVMOdPHuSNcYmxj2hITZfQ87E
AJafJXlrrUeoBrxOkTiHYwGSwj2e5EXPt3ECAwEAATCCATcGA1UdIwSCAS4wggEq
gIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANHglJHO6O56lMIp
DUCff+vMWhukXlnZTq4kS7wpiJdMSTYFzN8kOKLDRujLneAfyrYDW9Tm4xFYBZRX
uRiCq7H7ck+HFR0Xq5fuKzp5j/KaGxMbloSGOWqdcWvHOSMwAr4mmGui4le14nXI
fUYZcZuXwooEHSHOMkVrZUa/6M0lNNoTWvUCj74HAXJNAEu7D+hfcKOQ0E7WbSYR
Yol+Kjw3ij6HTGPIjUpY8emDd4tWkJx3pzuTUIABCAcqz9al/Ok3ac2ZEPQbM83N
4JbWrioj+ic8nkA+xvd8NYmdbfU9fiuUXOeQQMmUeAqlgmE3nC+zfSihLYaLJ61A
skTL7a0CAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAIswvhJX2y0YZiiFJy5b6KPID
SJuD+UZmIiDs61ZL6IK3iNRMnJX0IgTQMi5Ema1svvFCMM6502fMm5ktDSEr/GXY
KvqeJ4VmcgfCAxSE5tDek5/KpKOw1X57jqdoMJ25xLWTneN4YeTJDeTAG56T5/Ci
nzsQ7KiJuJc/zsKCgscZ/bQocbz3Pn9jPZTBktCdqsCvVOqHB0RuaclQxWfKsRN4
52Xov7EGYbm0xfifdg2HLnZFUzOnVAEQNsnKXW095IGFgUYlonuQ+jotRfLEf3hm
IgQuEdz+6WvdabYC1igIWN9od6fnoNI3NSRwuttvnJVWX4FkVnhu1YRdGdNkGg==
-----END CERTIFICATE-----"""

    def "refresh certificate with chain"() {
        CertificateEvent firedEvent

        given:
        def oracleCloudCertificationsConfiguration =  new OracleCloudCertificationsConfiguration("testId", 0, "testName", true)
        def mockCertificates = Mock(Certificates)
        def mockApplicationEventPublisher = Mock(ApplicationEventPublisher)

        def service = new OracleCloudCertificateService(
                oracleCloudCertificationsConfiguration, mockCertificates, mockApplicationEventPublisher)

        when:
        service.refreshCertificate()

        then:
        1 * mockCertificates.getCertificateBundle(*_) >> GetCertificateBundleResponse.builder()
                .certificateBundle(
                        CertificateBundleWithPrivateKey.builder()
                                .privateKeyPem(PRIVATE_KEY)
                                .certificateId("testId")
                                .serialNumber("test")
                                .timeCreated(new Date())
                                .certChainPem(CERTIFICATE_CHAIN_STRING)
                                .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                                .certificatePem(CERTIFICATE_STRING).build())
                .build()

        1 * mockApplicationEventPublisher.publishEvent(*_) >> {arguments -> firedEvent=arguments[0]}
        firedEvent != null
        firedEvent.privateKey() != null
        firedEvent.intermediate() != null
        firedEvent.intermediate().size() == 3
        firedEvent.certificate() != null
    }

    def "refresh certificate"() {
        CertificateEvent firedEvent
        given:
            def oracleCloudCertificationsConfiguration =  new OracleCloudCertificationsConfiguration("testId", 0, "testName", true)
            def mockCertificates = Mock(Certificates)
            def mockApplicationEventPublisher = Mock(ApplicationEventPublisher)

            def service = new OracleCloudCertificateService(
                    oracleCloudCertificationsConfiguration, mockCertificates, mockApplicationEventPublisher)

        when:
        service.refreshCertificate()

        then:
        1 * mockCertificates.getCertificateBundle(*_) >> GetCertificateBundleResponse.builder()
        .certificateBundle(
                CertificateBundleWithPrivateKey.builder()
                .privateKeyPem(PRIVATE_KEY)
                .certificateId("testId")
                .serialNumber("test")
                .timeCreated(new Date())
                .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                .certificatePem(CERTIFICATE_STRING).build())
        .build()

        1 * mockApplicationEventPublisher.publishEvent(*_) >> {arguments -> firedEvent=arguments[0]}
        firedEvent != null
        firedEvent.privateKey() != null
        firedEvent.intermediate() != null
        firedEvent.intermediate().size() == 0
        firedEvent.certificate() != null
    }

    def "refresh certificate with invalid private key"() {
        given:
        def oracleCloudCertificationsConfiguration =  new OracleCloudCertificationsConfiguration("testId", 0, "testName", true)
        def mockCertificates = Mock(Certificates)
        def mockApplicationEventPublisher = Mock(ApplicationEventPublisher)

        def service = new OracleCloudCertificateService(
                oracleCloudCertificationsConfiguration, mockCertificates, mockApplicationEventPublisher)

        when:
        service.refreshCertificate()

        then:
        1 * mockCertificates.getCertificateBundle(*_) >> GetCertificateBundleResponse.builder()
                .certificateBundle(
                        CertificateBundleWithPrivateKey.builder()
                                .privateKeyPem("Invalid private key")
                                .certificateId("testId")
                                .serialNumber("test")
                                .timeCreated(new Date())
                                .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                                .certificatePem(CERTIFICATE_STRING).build())
                .build()
        final IllegalStateException exception = thrown()
        exception.message == 'Unexpected value: null'
    }

    def "refresh certificate with invalid certificate"() {
        given:
        def oracleCloudCertificationsConfiguration =  new OracleCloudCertificationsConfiguration("testId", 0, "testName", true)
        def mockCertificates = Mock(Certificates)
        def mockApplicationEventPublisher = Mock(ApplicationEventPublisher)

        def service = new OracleCloudCertificateService(
                oracleCloudCertificationsConfiguration, mockCertificates, mockApplicationEventPublisher)

        when:
        service.refreshCertificate()

        then:
        1 * mockCertificates.getCertificateBundle(*_) >> GetCertificateBundleResponse.builder()
                .certificateBundle(
                        CertificateBundleWithPrivateKey.builder()
                                .privateKeyPem(PRIVATE_KEY)
                                .certificateId("testId")
                                .serialNumber("test")
                                .timeCreated(new Date())
                                .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                                .certificatePem("Invalid Cert").build())
                .build()
        0 * mockApplicationEventPublisher.publishEvent(*_)
    }

}
