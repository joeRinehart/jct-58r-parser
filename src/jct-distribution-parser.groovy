File raw = new File('../input/jct-distribution.txt')

Boolean payingAttention = false
Integer currentYear
List incomeBrackets = []

Map ignoredStarts = [
    'CHANGE IN FEDERAL TAXES (3)' : true,
    'INCOME FEDERAL' : true,
    'CATEGORY (2) TAXES (3)' : true,
    'Millions Percent Billions' : true
]

def fromCurrencyString = { String value, BigInteger extra ->
    BigDecimal intermediate = ( value.toString().replace('$', '').replaceAll(',', '')) as BigDecimal
    return intermediate * extra
}
def fromMillionsString = { String value ->
    return fromCurrencyString( value, 1000000)
}
def fromBillionsString = { String value ->
    return fromCurrencyString( value, 1000000000)
}
def fromPercentageString = { String value ->
    return ( (value.toString().replace('%', '').replace('(','').replace(')', '') as BigDecimal)  / 100)
}
def tokensToData = { List tokens ->
    String bracket = tokens.subList(0, 3).join(' ')
    if ( !incomeBrackets.contains(bracket) ) {
        incomeBrackets << bracket
    }


    BigInteger minIncome = 0
    if ( !tokens[0].startsWith('Total') && !tokens[0].startsWith('Less') ) {
        minIncome = fromCurrencyString(tokens[0], 1)
    }

    Map result = [
        income_bracket: bracket,
        min_income: minIncome,
        federal_tax_change_millions: fromMillionsString( tokens[3] ),
        federal_tax_change_percent: fromPercentageString( tokens[4] ),
        taxes_under_current_billions: fromBillionsString( tokens[5] ),
        taxes_under_current_percent: fromPercentageString( tokens[6] ),
        taxes_under_proposal_billions: fromBillionsString( tokens[7] ),
        taxes_under_proposal_percent: fromPercentageString( tokens[8] ),
        rate_under_current_percent: fromPercentageString( tokens[9] ),
        rate_under_proposal_percent: fromPercentageString( tokens[10] ),
    ]

    result.rate_change_under_proposal_percent = result.rate_under_proposal_percent - result.rate_under_current_percent
    result.min_cash_difference = result.min_income * result.rate_change_under_proposal_percent

    return result
}

Map statsByYear = [:]

raw.eachLine { line ->
    line = line.trim()
    if ( !payingAttention && line.startsWith("Calendar Year") ) {
        payingAttention = true
    } else if ( payingAttention && line.startsWith("Source: Joint Committee on Taxation") ) {
        payingAttention = false
    }

    if ( payingAttention ) {
        Boolean include = true
        // efficiency? it's a small file.
        ignoredStarts.eachWithIndex{ start, v, i ->
            if ( include ) {
                include = !line.startsWith(start)
            }
        }
        if ( include ) {
            if ( line.startsWith("Calendar Year") ) {
                currentYear = line.tokenize(' ').last() as Integer
                statsByYear[ currentYear ] = []
            } else {
                statsByYear[currentYear] << tokensToData( line.tokenize() )
                println tokensToData( line.tokenize() )
            }
        }
    }
}

// create simple tab delimit of years (cols) brackets (rows) and proposed tax rates
print 'Income Bracket\t2017\t'
statsByYear.eachWithIndex { year, v, i ->
    print year + '\t'
}
print '\n'
incomeBrackets.each{ bracket ->
    print bracket + '\t0\t'
    statsByYear.eachWithIndex{ year, v, i  ->
        def stat = v.find{
            it.income_bracket == bracket
        }
        print stat.rate_change_under_proposal_percent + '\t'
    }
    print '\n'
}

// tab delimit of  mincash difference
print 'Income Bracket\t2017\t'
statsByYear.eachWithIndex { year, v, i ->
    print year + '\t'
}
print '\n'
incomeBrackets.each{ bracket ->
    print bracket + '\t0\t'
    statsByYear.eachWithIndex{ year, v, i  ->
        def stat = v.find{
            it.income_bracket == bracket
        }
        print Math.round(stat.min_cash_difference / 12 ) + '\t'
    }
    print '\n'
}


