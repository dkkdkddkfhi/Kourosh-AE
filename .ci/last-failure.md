# Gradle failure on 2026-09-24 20:07:12 UTC

## Errors
```
4878:> Task :app:compileReleaseKotlin FAILED
4879:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/FontChoice.kt:48:20 Unresolved reference 'VAZIRMATN'.
4880:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/FontChoice.kt:48:38 Unresolved reference 'VAZIRMATN_BOLD'.
4881:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/FontChoice.kt:49:20 Unresolved reference 'NOTO_SANS'.
4882:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/FontChoice.kt:49:38 Unresolved reference 'NOTO_SANS_MEDIUM'.
4883:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/MainActivity.kt:738:45 Unresolved reference 'railGlyph'.
4884:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/MainActivity.kt:1498:69 Unresolved reference 'primaryText'.
4885:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:2415:26 Unresolved reference 'establish'.
4886:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:2533:22 Unresolved reference 'establish'.
4887:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:2693:22 Unresolved reference 'establish'.
4888:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:4136:26 Unresolved reference 'establish'.
4889:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:4266:22 Unresolved reference 'establish'.
4890:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:4560:18 Unresolved reference 'establish'.
4891:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5510:9 Missing return statement.
4892:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:9 Syntax error: Expecting member declaration.
4893:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:16 Syntax error: Expecting member declaration.
4894:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:23 Syntax error: Expecting member declaration.
4895:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:24 Syntax error: Expecting member declaration.
4896:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:32 Syntax error: Expecting member declaration.
4897:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:33 Syntax error: Expecting member declaration.
4898:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:41 Syntax error: Expecting member declaration.
4899:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:42 Syntax error: Expecting member declaration.
4900:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:46 Syntax error: Expecting member declaration.
4901:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:47 Syntax error: Expecting member declaration.
4902:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:63 Syntax error: Expecting member declaration.
4903:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:64 Syntax error: Expecting member declaration.
4904:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:70 Syntax error: Expecting member declaration.
4905:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5512:13 Syntax error: Expecting member declaration.
4906:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5512:14 Syntax error: Expecting member declaration.
4907:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5512:19 Syntax error: Expecting member declaration.
4908:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5512:19 Function declaration must have a name.
4909:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5512:35 Unresolved reference 'it'.
4910:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5515:17 Unresolved reference 'Builder'.
4911:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5515:48 Unresolved reference 'Builder'.
4912:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5516:48 Unresolved label.
4913:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5522:13 Unresolved reference 'addDisallowedApplication'.
4914:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5522:38 Unresolved reference 'packageName'.
4915:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5523:20 Unresolved reference 'Builder'.
4916:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5535:13 Unresolved reference 'addDisallowedApplication'.
4917:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5535:38 Unresolved reference 'packageName'.
4918:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5536:20 Unresolved reference 'Builder'.
4919:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5544:25 Unresolved reference 'addAllowedApplication'.
4920:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5548:36 Unresolved reference 'packageName'.
4921:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5549:29 Unresolved reference 'addDisallowedApplication'.
4922:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5555:23 Unresolved reference 'LOG_TAG'.
4923:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5557:23 Unresolved reference 'LOG_TAG'.
4924:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5567:13 Unresolved reference 'addDisallowedApplication'.
4925:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5567:38 Unresolved reference 'packageName'.
4926:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5571:16 Unresolved reference 'Builder'.
4927:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5595:17 Unresolved reference 'Builder'.
4928:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5598:8 Unresolved reference 'Builder'.
4929:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5599:41 Unresolved reference 'Builder'.
4930:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5602:20 Unresolved reference 'Builder'.
4931:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5625:13 Unresolved reference 'excludeRoute'.
4932:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5631:16 Unresolved reference 'Builder'.
4933:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5640:21 Unresolved reference 'profiled'.
4934:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5641:13 Unresolved reference 'not' for operator '!'.
4935:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5641:29 Unresolved reference 'LAN_BYPASS_PREF'.
4936:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5642:37 Unresolved reference 'LAN_BYPASS_PREF'.
4937:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5642:60 Cannot infer type for type parameter 'T'. Specify it explicitly.
4938:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5645:33 Unresolved reference 'LAN_BYPASS_PREF'.
4939:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5653:21 Unresolved reference 'profiled'.
4940:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5654:13 Unresolved reference 'not' for operator '!'.
4941:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5654:29 Unresolved reference 'IRAN_BYPASS_PREF'.
4942:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5655:37 Unresolved reference 'IRAN_BYPASS_PREF'.
4943:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5655:61 Cannot infer type for type parameter 'T'. Specify it explicitly.
4944:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5658:33 Unresolved reference 'IRAN_BYPASS_PREF'.
4945:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5683:17 Unresolved reference 'Builder'.
4946:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5683:44 Unresolved reference 'Builder'.
4947:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5684:42 Unresolved reference 'Builder'.
4948:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5687:20 Unresolved reference 'Builder'.
4949:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5691:16 Unresolved reference 'open'.
4950:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5691:57 Cannot infer type for type parameter 'T'. Specify it explicitly.
4951:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5691:57 Cannot infer type for type parameter 'R'. Specify it explicitly.
4952:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5691:57 Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch:
4954:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5691:63 Cannot infer type for type parameter 'T'. Specify it explicitly.
4955:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5692:34 Cannot infer type for value parameter 'line'. Specify it explicitly.
4956:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5694:61 Return type mismatch: expected 'ERROR CLASS: Cannot infer type variable TypeVariable(_L)', actual 'Unit'.
4957:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5696:59 Return type mismatch: expected 'ERROR CLASS: Cannot infer type variable TypeVariable(_L)', actual 'Unit'.
4958:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5698:21 Unresolved reference 'excludeRoute'.
4959:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5706:16 Unresolved reference 'Builder'.
4960:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5709:17 Unresolved reference 'Builder'.
4961:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5709:86 Unresolved reference 'Builder'.
4962:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5712:9 Unresolved reference 'addAddress'.
4963:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5713:9 Unresolved reference 'addRoute'.
4964:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5716:13 Unresolved reference 'addAddress'.
4965:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5717:13 Unresolved reference 'addRoute'.
4966:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5719:16 Unresolved reference 'Builder'.
4967:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5722:17 Unresolved reference 'Builder'.
4968:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5725:8 Unresolved reference 'Builder'.
4969:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5726:70 Unresolved reference 'Builder'.
4970:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5727:75 Unresolved reference 'Builder'.
4971:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5729:13 Unresolved reference 'setHttpProxy'.
4972:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5734:16 Unresolved reference 'Builder'.
4973:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5777:17 Unresolved reference 'Builder'.
4974:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5777:90 Unresolved reference 'Builder'.
4975:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5827:26 Unresolved reference 'addDnsServer'.
4976:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5830:29 Unresolved reference 'addDnsServer'.
4977:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5835:13 Cannot infer type for type parameter 'R'. Specify it explicitly.
4978:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5835:27 Unresolved reference 'addDnsServer'.
4979:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5844:16 Unresolved reference 'Builder'.
4980:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5846:1 Syntax error: Expecting a top level declaration.
4982:FAILURE: Build failed with an exception.
4984:* What went wrong:
4985:Execution failed for task ':app:compileReleaseKotlin'.
```

## Tail
```

> Task :app:compileReleaseKotlin FAILED
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/FontChoice.kt:48:20 Unresolved reference 'VAZIRMATN'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/FontChoice.kt:48:38 Unresolved reference 'VAZIRMATN_BOLD'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/FontChoice.kt:49:20 Unresolved reference 'NOTO_SANS'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/FontChoice.kt:49:38 Unresolved reference 'NOTO_SANS_MEDIUM'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/MainActivity.kt:738:45 Unresolved reference 'railGlyph'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/MainActivity.kt:1498:69 Unresolved reference 'primaryText'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:2415:26 Unresolved reference 'establish'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:2533:22 Unresolved reference 'establish'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:2693:22 Unresolved reference 'establish'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:4136:26 Unresolved reference 'establish'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:4266:22 Unresolved reference 'establish'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:4560:18 Unresolved reference 'establish'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5510:9 Missing return statement.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:9 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:16 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:23 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:24 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:32 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:33 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:41 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:42 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:46 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:47 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:63 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:64 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5511:70 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5512:13 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5512:14 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5512:19 Syntax error: Expecting member declaration.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5512:19 Function declaration must have a name.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5512:35 Unresolved reference 'it'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5515:17 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5515:48 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5516:48 Unresolved label.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5522:13 Unresolved reference 'addDisallowedApplication'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5522:38 Unresolved reference 'packageName'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5523:20 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5535:13 Unresolved reference 'addDisallowedApplication'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5535:38 Unresolved reference 'packageName'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5536:20 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5544:25 Unresolved reference 'addAllowedApplication'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5548:36 Unresolved reference 'packageName'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5549:29 Unresolved reference 'addDisallowedApplication'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5555:23 Unresolved reference 'LOG_TAG'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5557:23 Unresolved reference 'LOG_TAG'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5567:13 Unresolved reference 'addDisallowedApplication'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5567:38 Unresolved reference 'packageName'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5571:16 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5595:17 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5598:8 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5599:41 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5602:20 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5625:13 Unresolved reference 'excludeRoute'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5631:16 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5640:21 Unresolved reference 'profiled'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5641:13 Unresolved reference 'not' for operator '!'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5641:29 Unresolved reference 'LAN_BYPASS_PREF'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5642:37 Unresolved reference 'LAN_BYPASS_PREF'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5642:60 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5645:33 Unresolved reference 'LAN_BYPASS_PREF'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5653:21 Unresolved reference 'profiled'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5654:13 Unresolved reference 'not' for operator '!'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5654:29 Unresolved reference 'IRAN_BYPASS_PREF'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5655:37 Unresolved reference 'IRAN_BYPASS_PREF'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5655:61 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5658:33 Unresolved reference 'IRAN_BYPASS_PREF'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5683:17 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5683:44 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5684:42 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5687:20 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5691:16 Unresolved reference 'open'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5691:57 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5691:57 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5691:57 Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch:
fun <T : Closeable?, R> T.use(block: (T) -> R): R
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5691:63 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5692:34 Cannot infer type for value parameter 'line'. Specify it explicitly.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5694:61 Return type mismatch: expected 'ERROR CLASS: Cannot infer type variable TypeVariable(_L)', actual 'Unit'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5696:59 Return type mismatch: expected 'ERROR CLASS: Cannot infer type variable TypeVariable(_L)', actual 'Unit'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5698:21 Unresolved reference 'excludeRoute'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5706:16 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5709:17 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5709:86 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5712:9 Unresolved reference 'addAddress'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5713:9 Unresolved reference 'addRoute'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5716:13 Unresolved reference 'addAddress'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5717:13 Unresolved reference 'addRoute'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5719:16 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5722:17 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5725:8 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5726:70 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5727:75 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5729:13 Unresolved reference 'setHttpProxy'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5734:16 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5777:17 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5777:90 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5827:26 Unresolved reference 'addDnsServer'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5830:29 Unresolved reference 'addDnsServer'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5835:13 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5835:27 Unresolved reference 'addDnsServer'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5844:16 Unresolved reference 'Builder'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt:5846:1 Syntax error: Expecting a top level declaration.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileReleaseKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 9m 21s
52 actionable tasks: 52 executed
```
