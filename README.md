
# Web3 Authentication

Web3 authentication implemented in Scala 3 on top of ZIO

## Business logic:
- User will be identified from his/her wallet address
> Current version only supports evm wallets (ETH, BSC, etc.)
- User needs to request a challenge before login
- User has to sign the challenge with its private key
- Signature is validated and a token is created with all relevant information about the user
> Token format supported: JWT
- Token can be validated and then extract the information about the user (claims)
> Claims example { "sub": "urn:wallet:$wallet-address", "iac": 123456 }
